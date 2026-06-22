package com.marcm.middleearthjourney

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.marcm.middleearthjourney.data.StepRepository
import com.marcm.middleearthjourney.service.StepTrackingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repo: StepRepository
        get() = (application as MiddleEarthApp).stepRepository

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(repo) as T
        }
    }

    private val requestActivityRecognition = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) onActivityRecognitionGranted()
    }

    private val requestPostNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Si lo deniega, el servicio sigue funcionando pero sin notificación visible. */ }

    // Permiso de lectura de pasos de Health Connect (agrega móvil + smartwatch).
    private val healthPermissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    private val requestHealthConnect = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted: Set<String> ->
        if (granted.containsAll(healthPermissions)) {
            lifecycleScope.launch {
                if (repo.enableHealthConnect()) StepTrackingService.start(this@MainActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureActivityRecognitionPermission()
        ensureNotificationPermission()
        ensureHealthConnect()
        setContent {
            App(viewModel = viewModel, onRequestPermission = { ensureActivityRecognitionPermission() })
        }
    }

    private var foregroundRefreshJob: Job? = null

    override fun onResume() {
        super.onResume()
        // Mientras la app está en primer plano, releemos Health Connect periódicamente para ir
        // recogiendo los pasos que el smartwatch va sincronizando (repo.refresh() es no-op en
        // modo sensor). El ritmo de actualización real lo marca la sincronización del reloj.
        foregroundRefreshJob?.cancel()
        foregroundRefreshJob = lifecycleScope.launch {
            while (true) {
                repo.refresh()
                delay(FOREGROUND_REFRESH_MS)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        foregroundRefreshJob?.cancel()
        foregroundRefreshJob = null
    }

    private fun onActivityRecognitionGranted() {
        viewModel.onPermissionGranted()
        StepTrackingService.start(this)
    }

    private fun ensureActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val perm = Manifest.permission.ACTIVITY_RECOGNITION
            val granted = ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                onActivityRecognitionGranted()
            } else {
                requestActivityRecognition.launch(perm)
            }
        } else {
            onActivityRecognitionGranted()
        }
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val perm = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestPostNotifications.launch(perm)
    }

    /**
     * Si Health Connect está disponible, intenta activarlo (si ya tenemos permiso) o lo solicita.
     * Es la fuente preferida: cuenta los pasos del móvil y del smartwatch ya deduplicados.
     */
    private fun ensureHealthConnect() {
        if (!repo.healthConnectAvailable) return
        lifecycleScope.launch {
            if (repo.enableHealthConnect()) {
                StepTrackingService.start(this@MainActivity)
            } else {
                requestHealthConnect.launch(healthPermissions)
            }
        }
    }

    private companion object {
        const val FOREGROUND_REFRESH_MS = 10_000L // refresco en primer plano cada 10 s
    }
}
