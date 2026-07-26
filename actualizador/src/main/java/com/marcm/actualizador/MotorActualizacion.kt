package com.marcm.actualizador

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Cómo terminó el intento de aplicar una actualización. */
sealed interface ResultadoInstalacion {
    /**
     * La instalación se lanzó de verdad. Por [Via.SESION] el resultado final llegará
     * por [InstallResultReceiver]; por [Via.INTENT] no llega nada y lo resuelve la
     * fachada al volver a primer plano.
     */
    data class Lanzada(val via: Via) : ResultadoInstalacion

    /** No se llegó a instalar nada. El APK problemático ya está borrado. */
    data class Fallo(val tipo: TipoError, val mensaje: String? = null) : ResultadoInstalacion
}

/**
 * Núcleo del flujo de actualización: descarga (o reutiliza) el APK, verifica su
 * SHA-256 y solo entonces lo entrega al instalador. No conoce Android — trabaja con
 * un directorio, un [Descargador] y un [InstaladorApk] —, así que el camino completo
 * se prueba en la JVM con dobles, incluido el tramo final que antes solo se podía
 * validar en un dispositivo.
 *
 * Invariante: **nunca se instala un fichero cuyo hash no coincida con el manifiesto**.
 * El `.part` en curso jamás se instala; el `.apk` definitivo solo nace tras verificar.
 */
class MotorActualizacion(
    private val dirDescargas: File,
    private val instalador: InstaladorApk,
    private val descargador: Descargador = Descargador(),
) {
    /**
     * Aplica [info]: reutiliza un APK ya verificado si lo hay, si no lo descarga,
     * comprueba el hash e instala. Va emitiendo el avance por [onEstado] para la UI.
     */
    suspend fun aplicar(
        info: InfoActualizacion,
        onEstado: (EstadoActualizacion) -> Unit = {},
    ): ResultadoInstalacion {
        val part = File(dirDescargas, "${info.versionCode}.apk.part")
        val apk = File(dirDescargas, "${info.versionCode}.apk")

        withContext(Dispatchers.IO) {
            dirDescargas.mkdirs()
            CacheDescargas.limpiar(dirDescargas, info.versionCode)
        }

        // Un APK ya descargado y verificado se reutiliza: pasa si el proceso murió
        // justo antes de instalar o si el usuario canceló la confirmación del sistema.
        // Si el hash ya no cuadra, verificarOBorrar lo borra y se vuelve a descargar.
        val listo = withContext(Dispatchers.IO) {
            apk.isFile && VerificadorSha.verificarOBorrar(apk, info.sha256)
        }

        if (!listo) {
            onEstado(EstadoActualizacion.Descargando(0))
            val res = descargador.descargar(info.url, part) { pct ->
                onEstado(EstadoActualizacion.Descargando(pct))
            }
            if (res !is ResultadoDescarga.Ok) {
                return ResultadoInstalacion.Fallo(TipoError.DESCARGA)
            }

            onEstado(EstadoActualizacion.Verificando)
            val correcto = withContext(Dispatchers.IO) {
                VerificadorSha.verificarOBorrar(part, info.sha256)
            }
            if (!correcto) {
                return ResultadoInstalacion.Fallo(TipoError.HASH)
            }
            // El .apk definitivo solo nace tras verificar: nunca hay en disco un APK
            // instalable sin comprobar.
            withContext(Dispatchers.IO) {
                apk.delete()
                part.renameTo(apk)
            }
        }

        onEstado(EstadoActualizacion.Instalando)
        return try {
            ResultadoInstalacion.Lanzada(instalador.instalar(apk))
        } catch (e: Exception) {
            ResultadoInstalacion.Fallo(TipoError.INSTALACION, e.message)
        }
    }
}
