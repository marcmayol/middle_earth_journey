package com.marcm.middleearthjourney

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.marcm.middleearthjourney.ui.AppTheme
import com.marcm.middleearthjourney.ui.MainScreen
import com.marcm.middleearthjourney.ui.SplashScreen
import kotlinx.coroutines.delay

/**
 * Raíz de la app, compartida por Android (MainActivity) e iOS (MainViewController).
 * Recoge el estado del ViewModel y monta MainScreen + el splash.
 *
 * @param onRequestPermission acción de plataforma para pedir el permiso de actividad/pasos
 *        (en Android lanza el permiso ACTIVITY_RECOGNITION; en iOS, el de CoreMotion).
 */
@Composable
fun App(viewModel: MainViewModel, onRequestPermission: () -> Unit = {}) {
    AppTheme {
        val state by viewModel.state.collectAsState()
        val stats by viewModel.stats.collectAsState()
        val permissionGranted by viewModel.permissionGranted.collectAsState()
        val routeChosen by viewModel.routeChosen.collectAsState()
        val cineSeen by viewModel.cineSeen.collectAsState()
        val pendingEvent by viewModel.pendingEvent.collectAsState()
        val eventLog by viewModel.eventLog.collectAsState()

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
                onRequestPermission = onRequestPermission,
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
