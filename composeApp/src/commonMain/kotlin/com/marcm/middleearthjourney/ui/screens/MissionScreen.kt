package com.marcm.middleearthjourney.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcm.middleearthjourney.JourneyState
import com.marcm.middleearthjourney.data.JourneyEvent
import com.marcm.middleearthjourney.ui.CellBg
import com.marcm.middleearthjourney.ui.CodexCard
import com.marcm.middleearthjourney.ui.CardQuietBrush
import com.marcm.middleearthjourney.ui.Ember
import com.marcm.middleearthjourney.ui.Eyebrow
import com.marcm.middleearthjourney.ui.GoldBright
import com.marcm.middleearthjourney.ui.GoldDeep
import com.marcm.middleearthjourney.ui.GoldDivider
import com.marcm.middleearthjourney.ui.HoyGoldBrush
import com.marcm.middleearthjourney.ui.OnHoy
import com.marcm.middleearthjourney.ui.ProgressBar
import com.marcm.middleearthjourney.ui.TextBody
import com.marcm.middleearthjourney.ui.TextFaint
import com.marcm.middleearthjourney.ui.TextPrimary
import com.marcm.middleearthjourney.ui.TextSecondary
import com.marcm.middleearthjourney.ui.intEs
import com.marcm.middleearthjourney.ui.kmEs
import com.marcm.middleearthjourney.util.fmtDayMonth
import kotlin.math.roundToLong

@Composable
fun MissionScreen(state: JourneyState, quote: String, events: List<JourneyEvent> = emptyList()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Header(state) }
        item { TodayCard(state) }
        if (state.showCalories) {
            item { CaloriesTodayCard(state) }
        }
        item { Hero(state) }
        item { GlobalProgress(state) }
        if (state.nextWaypoint != null) {
            item { NextDestinationCard(state) }
        }
        item { DataGrid(state) }
        item { QuoteCard(quote) }
        if (events.isNotEmpty()) {
            item { Eyebrow("Diario del camino", modifier = Modifier.padding(top = 6.dp)) }
            items(events) { ev -> EventCard(ev) }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun EventCard(event: JourneyEvent) {
    CodexCard(
        modifier = Modifier.fillMaxWidth(),
        brush = CardQuietBrush,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(event.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp), color = GoldBright)
        Spacer(Modifier.height(6.dp))
        Text(event.body, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

@Composable
private fun Header(state: JourneyState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.routeTitle,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp),
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Eyebrow(
            text = if (state.daysElapsed > 0) "Día ${state.daysElapsed} del viaje" else "Tu viaje comienza",
            align = TextAlign.Center,
        )
    }
}

@Composable
private fun TodayCard(state: JourneyState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(HoyGoldBrush)
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Column {
            Eyebrow("Hoy", color = OnHoy.copy(alpha = 0.7f))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TodayMetric(intEs(state.todaySteps), "pasos", Modifier.weight(1f))
                Box(
                    Modifier
                        .height(40.dp)
                        .width(1.dp)
                        .background(OnHoy.copy(alpha = 0.3f)),
                )
                TodayMetric(kmEs(state.todayKm, 2), "km", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CaloriesTodayCard(state: JourneyState) {
    CodexCard(
        modifier = Modifier.fillMaxWidth(),
        brush = CardQuietBrush,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CellBg)
                    .border(1.dp, GoldDivider, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = Ember,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Eyebrow("Calorías hoy")
                Spacer(Modifier.height(2.dp))
                Text(
                    "Estimación · se reinicia a medianoche",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextFaint,
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    intEs(state.todayCalories),
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 30.sp),
                    color = GoldBright,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun TodayMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 36.sp),
            color = OnHoy,
        )
        Eyebrow(label, color = OnHoy.copy(alpha = 0.7f))
    }
}

@Composable
private fun Hero(state: JourneyState) {
    val remaining = (state.totalKm - state.km).coerceAtLeast(0.0)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = intEs(remaining.roundToLong()),
            style = MaterialTheme.typography.displayLarge,
            color = GoldBright,
            textAlign = TextAlign.Center,
        )
        Eyebrow("Kilómetros", color = TextSecondary, align = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (state.finished) state.arrivedLabel else state.goalLabel,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GlobalProgress(state: JourneyState) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Eyebrow("Progreso global")
            Text(
                "${kmEs(state.progress * 100.0, 1)} %",
                style = MaterialTheme.typography.titleMedium,
                color = GoldBright,
            )
        }
        Spacer(Modifier.height(10.dp))
        ProgressBar(progress = state.progress)
        Spacer(Modifier.height(8.dp))
        Text(
            "${kmEs(state.km)} / ${kmEs(state.totalKm, 0)} km recorridos",
            style = MaterialTheme.typography.bodySmall,
            color = TextFaint,
        )
    }
}

@Composable
private fun NextDestinationCard(state: JourneyState) {
    val next = state.nextWaypoint ?: return
    val kmDone = (state.legTotalKm - state.kmToNext).coerceAtLeast(0.0)
    CodexCard(
        modifier = Modifier.fillMaxWidth(),
        brush = CardQuietBrush,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Próximo destino")
                Spacer(Modifier.height(4.dp))
                Text(next.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
            Text(
                "${kmEs(state.kmToNext)} km",
                style = MaterialTheme.typography.titleMedium,
                color = GoldBright,
            )
        }
        Spacer(Modifier.height(12.dp))
        ProgressBar(progress = state.legProgress, height = 5.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            "${kmEs(kmDone)} / ${kmEs(state.legTotalKm, 0)} km del tramo",
            style = MaterialTheme.typography.bodySmall,
            color = TextFaint,
        )
    }
}

@Composable
private fun DataGrid(state: JourneyState) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            DataCell(
                "Iniciada",
                state.missionStart?.let { fmtDayMonth(it) } ?: "—",
                Modifier.weight(1f),
            )
            DataCell(
                "Llegada est.",
                state.estimatedArrival?.let { fmtDayMonth(it) } ?: "—",
                Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            DataCell(
                "Ritmo medio",
                if (state.avgKmPerDay > 0) "${kmEs(state.avgKmPerDay)} km/día" else "—",
                Modifier.weight(1f),
            )
            DataCell(
                "Pasos totales",
                intEs(state.steps),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DataCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CellBg)
            .border(1.dp, GoldDivider, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Eyebrow(label)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = GoldBright)
    }
}

@Composable
private fun QuoteCard(quote: String) {
    CodexCard(
        modifier = Modifier.fillMaxWidth(),
        brush = CardQuietBrush,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = "“",
            style = MaterialTheme.typography.displayMedium,
            color = GoldDeep,
            modifier = Modifier.height(34.dp),
        )
        Text(
            text = quote,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
            color = TextBody,
            fontStyle = FontStyle.Italic,
        )
        Spacer(Modifier.height(10.dp))
        Eyebrow("Cita del día", color = GoldDeep)
    }
}
