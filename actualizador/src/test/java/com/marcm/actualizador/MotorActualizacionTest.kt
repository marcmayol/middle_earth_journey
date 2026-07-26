package com.marcm.actualizador

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * El flujo completo descarga → verificación → instalación, sin red y sin dispositivo:
 * el servidor es un [AbridorConexion] que sirve el APK desde memoria y el instalador
 * es un doble que solo apunta qué fichero le entregan. Aquí se comprueba lo que antes
 * solo podía verse en un móvil: que **un hash correcto llega a la instalación y uno
 * incorrecto no**.
 */
class MotorActualizacionTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val cuerpoApk = ByteArray(300_000) { (it % 256).toByte() }

    private fun sha256Hex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in MessageDigest.getInstance("SHA-256").digest(bytes)) {
            sb.append("%02x".format(b.toInt() and 0xFF))
        }
        return sb.toString()
    }

    private fun infoCon(sha: String) = InfoActualizacion(
        versionCode = 7,
        versionName = "1.4",
        url = "https://irrelevante/app.apk",
        sha256 = sha,
        notas = "Notas de la versión",
    )

    private fun servidorCon(codigo: Int, cuerpo: ByteArray) = AbridorConexion {
        object : Conexion {
            override val codigo = codigo
            override val longitud = cuerpo.size.toLong()
            override val flujo: InputStream = ByteArrayInputStream(cuerpo)
            override fun close() {}
        }
    }

    /** Instalador doble: no instala nada, apunta con qué fichero se le llamó. */
    private class InstaladorEspia(private val via: Via = Via.SESION) : InstaladorApk {
        val instalados = mutableListOf<File>()
        override fun instalar(apk: File): Via {
            instalados += apk
            return via
        }
    }

    private fun motorCon(
        dir: File,
        instalador: InstaladorApk,
        codigo: Int = 200,
        cuerpo: ByteArray = cuerpoApk,
    ) = MotorActualizacion(dir, instalador, Descargador(servidorCon(codigo, cuerpo)))

    @Test
    fun `hash correcto descarga, verifica e instala el APK verificado`() = runBlocking {
        val dir = temp.newFolder("actualizaciones")
        val espia = InstaladorEspia()
        val estados = mutableListOf<EstadoActualizacion>()

        val res = motorCon(dir, espia).aplicar(infoCon(sha256Hex(cuerpoApk))) { estados += it }

        assertEquals(ResultadoInstalacion.Lanzada(Via.SESION), res)
        assertEquals("debe instalarse exactamente una vez", 1, espia.instalados.size)

        val instalado = espia.instalados.single()
        assertEquals("7.apk", instalado.name)
        assertTrue("el APK entregado debe existir", instalado.isFile)
        assertEquals(cuerpoApk.size.toLong(), instalado.length())
        assertTrue(
            "el fichero instalado es el verificado",
            VerificadorSha.verificarOBorrar(instalado, sha256Hex(cuerpoApk)),
        )
        assertFalse("no debe quedar un .part", File(dir, "7.apk.part").exists())

        // La UI ve el avance completo y en orden: progreso, verificación e instalación.
        assertTrue(estados.first() is EstadoActualizacion.Descargando)
        assertTrue(estados.contains(EstadoActualizacion.Descargando(100)))
        assertEquals(
            listOf(EstadoActualizacion.Verificando, EstadoActualizacion.Instalando),
            estados.takeLast(2),
        )
    }

    @Test
    fun `hash incorrecto borra el APK y no llega a instalar`() = runBlocking {
        val dir = temp.newFolder("actualizaciones")
        val espia = InstaladorEspia()

        val res = motorCon(dir, espia).aplicar(infoCon("c".repeat(64)))

        assertEquals(ResultadoInstalacion.Fallo(TipoError.HASH), res)
        assertTrue("jamás debe instalarse un APK sin verificar", espia.instalados.isEmpty())
        assertFalse(File(dir, "7.apk.part").exists())
        assertFalse(File(dir, "7.apk").exists())
    }

    @Test
    fun `fallo http al descargar no instala nada y no deja ficheros`() = runBlocking {
        val dir = temp.newFolder("actualizaciones")
        val espia = InstaladorEspia()

        val res = motorCon(dir, espia, codigo = 404, cuerpo = ByteArray(0))
            .aplicar(infoCon(sha256Hex(cuerpoApk)))

        assertEquals(ResultadoInstalacion.Fallo(TipoError.DESCARGA), res)
        assertTrue(espia.instalados.isEmpty())
        assertFalse(File(dir, "7.apk.part").exists())
    }

    @Test
    fun `un APK ya verificado en cache se reutiliza sin volver a descargar`() = runBlocking {
        val dir = temp.newFolder("actualizaciones")
        val espia = InstaladorEspia()
        File(dir, "7.apk").writeBytes(cuerpoApk)

        // Si el motor intentara descargar, este abridor haría fallar el test.
        val servidorProhibido = AbridorConexion { error("no debería descargar") }
        val motor = MotorActualizacion(dir, espia, Descargador(servidorProhibido))

        val res = motor.aplicar(infoCon(sha256Hex(cuerpoApk)))

        assertEquals(ResultadoInstalacion.Lanzada(Via.SESION), res)
        assertEquals(1, espia.instalados.size)
    }

    @Test
    fun `si el instalador falla se informa sin perder el APK verificado`() = runBlocking {
        val dir = temp.newFolder("actualizaciones")
        val instaladorRoto = InstaladorApk { error("sesión rechazada") }

        val res = motorCon(dir, instaladorRoto).aplicar(infoCon(sha256Hex(cuerpoApk)))

        assertTrue(res is ResultadoInstalacion.Fallo)
        assertEquals(TipoError.INSTALACION, (res as ResultadoInstalacion.Fallo).tipo)
        assertTrue("el APK verificado se conserva para reintentar", File(dir, "7.apk").isFile)
    }

    @Test
    fun `la via por intent se propaga para que la fachada la resuelva`() = runBlocking {
        val dir = temp.newFolder("actualizaciones")
        val espia = InstaladorEspia(Via.INTENT)

        val res = motorCon(dir, espia).aplicar(infoCon(sha256Hex(cuerpoApk)))

        assertEquals(ResultadoInstalacion.Lanzada(Via.INTENT), res)
    }
}
