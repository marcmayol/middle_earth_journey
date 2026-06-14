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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marcm.middleearthjourney.service.StepTrackingService

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                MainViewModel((application as MiddleEarthApp).stepRepository) as T
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureActivityRecognitionPermission()
        ensureNotificationPermission()
        setContent {
            App(viewModel = viewModel, onRequestPermission = { ensureActivityRecognitionPermission() })
        }
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
}
