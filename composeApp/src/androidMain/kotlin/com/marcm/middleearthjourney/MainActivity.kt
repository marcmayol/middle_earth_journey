package com.marcm.middleearthjourney

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.marcm.middleearthjourney.service.StepTrackingService
import com.marcm.middleearthjourney.ui.AppTheme
import com.marcm.middleearthjourney.ui.MainScreen
import com.marcm.middleearthjourney.ui.SplashScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

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
            AppTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val stats by viewModel.stats.collectAsStateWithLifecycle()
                val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
                val routeChosen by viewModel.routeChosen.collectAsStateWithLifecycle()
                val cineSeen by viewModel.cineSeen.collectAsStateWithLifecycle()
                val pendingEvent by viewModel.pendingEvent.collectAsStateWithLifecycle()
                val eventLog by viewModel.eventLog.collectAsStateWithLifecycle()
                LaunchedEffect(permissionGranted) {
                    if (permissionGranted) StepTrackingService.start(this@MainActivity)
                }
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1400)
                    showSplash = false
                }
                Box(Modifier.fillMaxSize()) {
                    MainScreen(
                        state = state,
                        stats = stats,
                        hasSensor = viewModel.hasSensor,
                        permissionGranted = permissionGranted,
                        routeChosen = routeChosen,
                        quote = viewModel.quoteOfTheDay,
                        onRequestPermission = { ensureActivityRecognitionPermission() },
                        onSelectRoute = { route -> viewModel.selectRoute(route) },
                        onStartReturn = { viewModel.startReturnJourney() },
                        cineSeen = cineSeen,
                        onMarkCineSeen = { viewModel.markCinematicsSeen(it) },
                        pendingEvent = pendingEvent,
                        onDismissEvent = { viewModel.dismissEvent() },
                        eventLog = eventLog,
                    )
                    AnimatedVisibility(
                        visible = showSplash,
                        enter = EnterTransition.None,
                        exit = fadeOut(tween(600)),
                    ) {
                        SplashScreen()
                    }
                }
            }
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
