package com.marcm.middleearthjourney.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.marcm.middleearthjourney.JourneyState
import com.marcm.middleearthjourney.data.RouteId
import com.marcm.middleearthjourney.data.Routes
import com.marcm.middleearthjourney.ui.CodexCard
import com.marcm.middleearthjourney.ui.CardQuietBrush
import com.marcm.middleearthjourney.ui.CodexDialog
import com.marcm.middleearthjourney.ui.Eyebrow
import com.marcm.middleearthjourney.ui.GoldBorder
import com.marcm.middleearthjourney.ui.GoldBright
import com.marcm.middleearthjourney.ui.MapPaper
import com.marcm.middleearthjourney.ui.MiddleEarthMap
import com.marcm.middleearthjourney.ui.OutlinePill
import com.marcm.middleearthjourney.ui.TextBody
import com.marcm.middleearthjourney.ui.TextPrimary
import com.marcm.middleearthjourney.ui.TextSecondary
import com.marcm.middleearthjourney.ui.kmEs

@Composable
fun MapScreen(state: JourneyState, onSelectRoute: (RouteId) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                "Mapa de la Tierra Media",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
        }
        item { RouteSelectorCard(state, onSelectRoute) }
        item { StageRow(state) }
        item { MapFrame(state) }
        item { CurrentWaypointCard(state) }
        state.nextWaypoint?.let { item { NextWaypointCard(state) } }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun RouteSelectorCard(state: JourneyState, onSelectRoute: (RouteId) -> Unit) {
    var pendingRoute by remember { mutableStateOf<RouteId?>(null) }

    CodexCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Ruta activa")
                Spacer(Modifier.height(4.dp))
                Text(state.routeTitle, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
            val other = if (state.routeId == RouteId.FRODO) RouteId.BILBO else RouteId.FRODO
            OutlinePill("Cambiar", onClick = { pendingRoute = other })
        }
    }

    pendingRoute?.let { target ->
        val targetDef = Routes.byId(target)
        CodexDialog(
            title = "Cambiar a «${targetDef.title}»",
            message = "Empezarás la ruta de ${targetDef.goalName} desde cero. " +
                "Se perderá el progreso de tu ruta actual («${state.routeTitle}»). " +
                "Tus pasos totales y tus estadísticas no se ven afectados.",
            confirmText = "Cambiar ruta",
            onConfirm = {
                onSelectRoute(target)
                pendingRoute = null
            },
            dismissText = "Cancelar",
            onDismiss = { pendingRoute = null },
            onDismissRequest = { pendingRoute = null },
        )
    }
}

@Composable
private fun StageRow(state: JourneyState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Eyebrow("Etapa actual")
            Spacer(Modifier.height(3.dp))
            Text(state.currentWaypoint.region, style = MaterialTheme.typography.headlineSmall, color = GoldBright)
        }
        if (state.daysElapsed > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Eyebrow("Día")
                Spacer(Modifier.height(3.dp))
                Text(state.daysElapsed.toString(), style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun MapFrame(state: JourneyState) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MapPaper)
            .border(1.dp, GoldBorder, shape),
    ) {
        MiddleEarthMap(
            waypoints = state.waypoints,
            routeId = state.routeId,
            currentIndex = state.currentIndex,
            progress = state.progress,
        )
    }
}

@Composable
private fun CurrentWaypointCard(state: JourneyState) {
    var expanded by remember { mutableStateOf(true) }
    CodexCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = GoldBright)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(state.currentWaypoint.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    state.currentWaypoint.region,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontStyle = FontStyle.Italic,
                )
            }
            OutlinePill(if (expanded) "Ocultar" else "Mostrar", onClick = { expanded = !expanded })
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(12.dp))
                Text(state.currentWaypoint.lore, style = MaterialTheme.typography.bodyLarge, color = TextBody)
            }
        }
    }
}

@Composable
private fun NextWaypointCard(state: JourneyState) {
    val next = state.nextWaypoint ?: return
    CodexCard(
        modifier = Modifier.fillMaxWidth(),
        brush = CardQuietBrush,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Flag, contentDescription = null, tint = GoldBright)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("Próximo destino")
                Spacer(Modifier.height(2.dp))
                Text(next.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
            Text("${kmEs(state.kmToNext)} km", style = MaterialTheme.typography.titleMedium, color = GoldBright)
        }
    }
}
