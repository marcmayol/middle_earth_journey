package com.marcm.middleearthjourney

import android.app.Application
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.ActualizadorConfig
import com.marcm.middleearthjourney.data.StepRepository

class MiddleEarthApp : Application() {

    lateinit var stepRepository: StepRepository
        private set

    /**
     * Auto-actualización: la app se distribuye fuera de Play Store, así que consulta
     * un manifiesto propio publicado en GitHub Pages (nunca la API de GitHub) y compara
     * por versionCode entero contra [BuildConfig.VERSION_CODE].
     */
    val actualizador: Actualizador by lazy {
        Actualizador(
            app = this,
            config = ActualizadorConfig(
                manifiestoUrl = URL_MANIFIESTO,
                versionCodeActual = BuildConfig.VERSION_CODE,
                checkHorasPorDefecto = 24,
            ),
        )
    }

    override fun onCreate() {
        super.onCreate()
        stepRepository = StepRepository(this)
        // Comprobación periódica en segundo plano (WorkManager, con red disponible).
        actualizador.programarPeriodica()
    }

    private companion object {
        const val URL_MANIFIESTO = "https://marcmayol.com/middle_earth_journey/updates.json"
    }
}
