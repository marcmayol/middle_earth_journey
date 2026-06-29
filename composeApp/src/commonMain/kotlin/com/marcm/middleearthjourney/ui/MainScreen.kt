package com.marcm.middleearthjourney.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcm.middleearthjourney.JourneyState
import com.marcm.middleearthjourney.StatsState
import com.marcm.middleearthjourney.data.JourneyEvent
import com.marcm.middleearthjourney.data.RouteId
import com.marcm.middleearthjourney.data.Routes
import com.marcm.middleearthjourney.ui.cinematic.CinematicPlayer
import com.marcm.middleearthjourney.ui.cinematic.collectionFor
import com.marcm.middleearthjourney.ui.screens.AchievementsScreen
import com.marcm.middleearthjourney.ui.screens.ChronicleScreen
import com.marcm.middleearthjourney.ui.screens.MapScreen
import com.marcm.middleearthjourney.ui.screens.MissionScreen
import com.marcm.middleearthjourney.ui.screens.SettingsScreen
import com.marcm.middleearthjourney.ui.screens.StatsScreen

private enum class Tab(val label: String, val icon: ImageVector) {
    Mission("Misión", Icons.Filled.Hiking),
    Map("Mapa", Icons.Filled.Map),
    Chronicle("Crónicas", Icons.Filled.AutoStories),
    Stats("Stats", Icons.Filled.BarChart),
    Achievements("Logros", Icons.Filled.EmojiEvents),
}

@Composable
fun MainScreen(
    state: JourneyState,
    stats: StatsState,
    hasSensor: Boolean,
    permissionGranted: Boolean,
    routeChosen: Boolean,
    quote: String,
    onRequestPermission: () -> Unit,
    onSelectRoute: (RouteId) -> Unit,
    onStartReturn: () -> Unit,
    cineSeen: Set<Int> = emptySet(),
    onMarkCineSeen: (Set<Int>) -> Unit = {},
    pendingEvent: JourneyEvent? = null,
    onDismissEvent: () -> Unit = {},
    eventLog: List<JourneyEvent> = emptyList(),
    weightKg: Int = 70,
    heightCm: Int = 170,
    showCalories: Boolean = true,
    onWeightChange: (Int) -> Unit = {},
    onHeightChange: (Int) -> Unit = {},
    onShowCaloriesChange: (Boolean) -> Unit = {},
) {
    var showChooser by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (!routeChosen || showChooser) {
        Box(Modifier.fillMaxSize().background(pageBackgroundBrush())) {
            RouteChooserScreen(onSelectRoute = { route ->
                showChooser = false
                onSelectRoute(route)
            })
        }
        return
    }

    var selected by rememberSaveable { mutableStateOf(Tab.Mission) }
    var playingChapter by remember(state.routeId, state.direction) { mutableStateOf<Int?>(null) }

    // Auto-lanzar la cinemática al cruzar un hito por primera vez (cada capítulo, una sola vez).
    val collection = collectionFor(state.routeId, state.direction)
    LaunchedEffect(state.routeId, state.direction, state.km, cineSeen, playingChapter) {
        if (playingChapter != null) return@LaunchedEffect
        val pending = collection.chapters.indices.filter { i ->
            state.km >= collection.chapters[i].unlockKm && i !in cineSeen
        }
        if (pending.isNotEmpty()) {
            // Suprime el "backlog": marca todos los alcanzados y reproduce solo el más reciente.
            onMarkCineSeen(pending.toSet())
            playingChapter = pending.last()
        }
    }

    Box(Modifier.fillMaxSize().background(pageBackgroundBrush())) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = { CodexBottomBar(selected) { selected = it } },
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                if (!hasSensor) {
                    DiscreetNotice("Este móvil no expone un sensor de pasos; el viaje no avanzará solo.")
                } else if (!permissionGranted) {
                    DiscreetNotice(
                        "Necesito el permiso de actividad para contar tus pasos.",
                        actionText = "Conceder",
                        onAction = onRequestPermission,
                    )
                }
                when (selected) {
                    Tab.Mission -> MissionScreen(state = state, quote = quote, events = eventLog)
                    Tab.Map -> MapScreen(state = state, onSelectRoute = onSelectRoute)
                    Tab.Chronicle -> ChronicleScreen(
                        routeId = state.routeId,
                        direction = state.direction,
                        km = state.km,
                        onPlayChapter = { playingChapter = it },
                    )
                    Tab.Stats -> StatsScreen(stats = stats)
                    Tab.Achievements -> AchievementsScreen(state = state)
                }
            }
        }

        // Acceso a Ajustes: engranaje discreto en la esquina superior derecha.
        if (playingChapter == null && !showSettings) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 14.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NavTop.copy(alpha = 0.55f))
                    .border(1.dp, GoldBorderSoft, RoundedCornerShape(20.dp))
                    .clickable { showSettings = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Ajustes",
                    tint = GoldBright,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        // Reproductor de cinemática a pantalla completa (sobre la barra de navegación)
        playingChapter?.let { chapter ->
            CinematicPlayer(
                collection = collectionFor(state.routeId, state.direction),
                startChapter = chapter,
                onExit = { playingChapter = null },
            )
        }

        // Overlay de Ajustes (sobre todo, incluida la barra inferior).
        if (showSettings) {
            SettingsScreen(
                weightKg = weightKg,
                heightCm = heightCm,
                showCalories = showCalories,
                onWeightChange = onWeightChange,
                onHeightChange = onHeightChange,
                onShowCaloriesChange = onShowCaloriesChange,
                onClose = { showSettings = false },
            )
        }
    }

    if (state.finished) {
        JourneyCompleteDialog(
            state = state,
            onStartReturn = onStartReturn,
            onChooseOther = { showChooser = true },
        )
    }

    // Suceso aleatorio del camino (no se solapa con la cinemática ni con el fin de ruta).
    if (pendingEvent != null && playingChapter == null && !state.finished) {
        CodexDialog(
            title = pendingEvent.title,
            message = pendingEvent.body,
            confirmText = "Continuar",
            onConfirm = onDismissEvent,
            onDismissRequest = onDismissEvent,
        )
    }
}

@Composable
private fun CodexBottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(NavTop, NavBottom)))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(GoldBorderSoft, Color.Transparent)),
                shape = RoundedCornerShape(0.dp),
            ),
    ) {
        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
            Tab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selected == tab,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(22.dp)) },
                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldBright,
                        selectedTextColor = GoldBright,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = GoldBase.copy(alpha = 0.16f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun DiscreetNotice(text: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardQuietBrush)
            .border(1.dp, GoldBorderSoft, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextBody, modifier = Modifier.weight(1f))
        if (actionText != null && onAction != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                actionText,
                style = MaterialTheme.typography.labelLarge,
                color = GoldBright,
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

@Composable
private fun JourneyCompleteDialog(
    state: JourneyState,
    onStartReturn: () -> Unit,
    onChooseOther: () -> Unit,
) {
    var dismissed by remember(state.routeId, state.direction) { mutableStateOf(false) }
    if (dismissed) return

    if (state.canStartReturn) {
        CodexDialog(
            title = "¡Ruta completada!",
            message = "Has llegado a ${state.destinationName}. ¿Quieres hacer el camino de vuelta a casa?",
            confirmText = "Sí, volver a casa",
            onConfirm = onStartReturn,
            dismissText = "Iniciar otra ruta",
            onDismiss = onChooseOther,
            dismissOutlined = true,
            onDismissRequest = { /* obliga a elegir */ },
        )
    } else {
        CodexDialog(
            title = "¡Has vuelto a casa!",
            message = "Has completado el viaje de ida y vuelta. ¿Quieres emprender una nueva ruta?",
            confirmText = "Elegir nueva ruta",
            onConfirm = onChooseOther,
            dismissText = "Ahora no",
            onDismiss = { dismissed = true },
            onDismissRequest = { dismissed = true },
        )
    }
}

// ───────────────────────── Onboarding ─────────────────────────

@Composable
private fun RouteChooserScreen(onSelectRoute: (RouteId) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Elige tu viaje",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Box(Modifier.width(60.dp).height(1.dp).background(GoldBase.copy(alpha = 0.5f)))
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Cada paso que des te acercará a tu destino en la Tierra Media. ¿Qué aventura quieres recorrer?",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(290.dp),
        )
        Spacer(Modifier.height(30.dp))

        RouteChoiceCard(
            routeId = RouteId.FRODO,
            title = Routes.FRODO.title,
            goalName = Routes.FRODO.goalName,
            totalKm = Routes.FRODO.totalKm,
            description = buildAnnotatedString {
                append("La marcha del Portador del Anillo y Sam hasta las Grietas del Destino. El camino largo y oscuro de ")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("El Señor de los Anillos") }
                append(".")
            },
            onClick = { onSelectRoute(RouteId.FRODO) },
        )
        Spacer(Modifier.height(16.dp))
        RouteChoiceCard(
            routeId = RouteId.BILBO,
            title = Routes.BILBO.title,
            goalName = Routes.BILBO.goalName,
            totalKm = Routes.BILBO.totalKm,
            description = buildAnnotatedString {
                append("La aventura inesperada de Bilbo con Thorin y los enanos hasta la Montaña Solitaria. El viaje de ")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("El Hobbit") }
                append(".")
            },
            onClick = { onSelectRoute(RouteId.BILBO) },
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Podrás cambiar de ruta más adelante desde el mapa.",
            style = MaterialTheme.typography.bodySmall,
            color = TextFaint,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun RouteChoiceCard(
    routeId: RouteId,
    title: String,
    goalName: String,
    totalKm: Double,
    description: androidx.compose.ui.text.AnnotatedString,
    onClick: () -> Unit,
) {
    val accent = routeColor(routeId)
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(CardBrush)
            .border(1.dp, GoldBorder, shape)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(Brush.verticalGradient(listOf(accent, routeColorDeep(routeId)))))
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp), color = GoldBright, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("${kmEs(totalKm, 0)} km", style = MaterialTheme.typography.labelLarge, color = accent)
                }
            }
            Spacer(Modifier.height(6.dp))
            Eyebrow("Bolsón Cerrado → $goalName", color = TextSecondary)
            Spacer(Modifier.height(10.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = TextBody)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                CtaButton("Empezar", onClick)
            }
        }
    }
}
