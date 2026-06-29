package com.marcm.middleearthjourney.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.marcm.middleearthjourney.StatsState
import com.marcm.middleearthjourney.ui.CellBg
import com.marcm.middleearthjourney.ui.CodexCard
import com.marcm.middleearthjourney.ui.CardQuietBrush
import com.marcm.middleearthjourney.ui.Ember
import com.marcm.middleearthjourney.ui.Eyebrow
import com.marcm.middleearthjourney.ui.GoldBase
import com.marcm.middleearthjourney.ui.GoldBright
import com.marcm.middleearthjourney.ui.GoldDeep
import com.marcm.middleearthjourney.ui.GoldDeep2
import com.marcm.middleearthjourney.ui.GoldDivider
import com.marcm.middleearthjourney.ui.HoyGold1
import com.marcm.middleearthjourney.ui.HoyGold2
import com.marcm.middleearthjourney.ui.Negative
import com.marcm.middleearthjourney.ui.TextBody
import com.marcm.middleearthjourney.ui.TextFaint
import com.marcm.middleearthjourney.ui.TextPrimary
import com.marcm.middleearthjourney.ui.TextSecondary
import com.marcm.middleearthjourney.ui.intEs
import com.marcm.middleearthjourney.util.compactNumber
import com.marcm.middleearthjourney.util.fmtDayMonth
import com.marcm.middleearthjourney.util.fmtLongDate
import com.marcm.middleearthjourney.util.kmEs
import com.marcm.middleearthjourney.util.pad2
import com.marcm.middleearthjourney.util.plusDays
import kotlinx.datetime.isoDayNumber
import kotlin.math.roundToLong

// Colores de barras
private val BarNormalTop = HoyGold2
private val BarNormalBottom = GoldDeep
private val BarPeakTop = HoyGold1
private val BarPeakBottom = GoldDeep2

@Composable
fun StatsScreen(stats: StatsState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenTitle("Estadísticas") }

        if (!stats.hasHistory) {
            item { NoticeCard("Aún no hay datos diarios. Tus pasos empezarán a registrarse por día desde hoy.") }
        } else if (stats.firstDayWithData != null) {
            item {
                NoticeCard(
                    "Datos diarios registrados desde el ${fmtLongDate(stats.firstDayWithData)}. " +
                        "Los días anteriores aparecen vacíos.",
                )
            }
        }

        if (stats.caloriesEnabled) {
            item { CaloriesCard(stats) }
        }
        item { HourlyCard(stats) }
        item { WeeklyCard(stats) }
        item { MonthlyCard(stats) }
        item { YearlyCard(stats) }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun ScreenTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        color = TextPrimary,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

@Composable
private fun NoticeCard(text: String) {
    CodexCard(brush = CardQuietBrush, contentPadding = PaddingValues(16.dp)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextBody)
    }
}

@Composable
private fun SectionHeader(eyebrow: String, sub: String?, total: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Eyebrow(eyebrow)
            if (sub != null) {
                Spacer(Modifier.height(3.dp))
                Text(sub, style = MaterialTheme.typography.bodySmall, color = TextFaint)
            }
        }
        Text(total, style = MaterialTheme.typography.titleMedium, color = GoldBright)
    }
}

@Composable
private fun HourlyCard(stats: StatsState) {
    val hourly = stats.hourlySteps
    val peakIdx = hourly.withIndex().filter { it.value > 0L }.maxByOrNull { it.value }?.index
    val activeHours = hourly.count { it > 0L }
    val perActiveHour = if (activeHours > 0) (stats.todaySteps.toDouble() / activeHours).roundToLong() else 0L

    CodexCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("Hoy, por horas", "Se reinicia a medianoche", "${intEs(stats.todaySteps)} pasos")
        Spacer(Modifier.height(14.dp))
        BarChart(
            values = hourly,
            labels = HOUR_LABELS,
            modifier = Modifier.fillMaxWidth().height(150.dp),
            compactLabels = true,
            showValues = false,
            peakIndex = peakIdx,
        )
        if (peakIdx != null) {
            Spacer(Modifier.height(12.dp))
            val peakLabel = "${pad2(peakIdx)}:00–${pad2((peakIdx + 1) % 24)}:00"
            Text(
                text = buildAnnotatedString {
                    append("Hora pico ")
                    withStyle(SpanStyle(color = GoldBright, fontWeight = FontWeight.SemiBold)) { append(peakLabel) }
                    append(" · ${intEs(hourly[peakIdx])} pasos")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
            )
            Text(
                "Media por hora activa: ${intEs(perActiveHour)} pasos/h",
                style = MaterialTheme.typography.bodySmall,
                color = TextFaint,
            )
        }
    }
}

@Composable
private fun WeeklyCard(stats: StatsState) {
    val weekEnd = plusDays(stats.weekStart, 6)
    val rangeLabel = "${fmtDayMonth(stats.weekStart)} — ${fmtDayMonth(weekEnd)}"

    CodexCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("Esta semana", rangeLabel, intEs(stats.weeklyTotal))
        Spacer(Modifier.height(14.dp))
        BarChart(
            values = stats.weeklySteps,
            labels = WEEK_LABELS,
            modifier = Modifier.fillMaxWidth().height(160.dp),
        )
        val bestDow = stats.bestDayOfWeek
        if (bestDow != null && stats.bestDaySteps > 0L) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = buildAnnotatedString {
                    append("Mejor día ")
                    withStyle(SpanStyle(color = GoldBright, fontWeight = FontWeight.SemiBold)) {
                        append(DOW_LONG[bestDow.isoDayNumber - 1])
                    }
                    append(" · ${intEs(stats.bestDaySteps)} pasos")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextBody,
            )
        }
    }
}

@Composable
private fun MonthlyCard(stats: StatsState) {
    CodexCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(stats.monthLabel.ifEmpty { "Este mes" }, null, intEs(stats.currentMonthSteps))
        Spacer(Modifier.height(10.dp))
        StatRow("Media diaria", "${intEs(stats.avgStepsPerDayThisMonth)} pasos/día")
        StatRow("Mes anterior", intEs(stats.previousMonthSteps))
        val diff = stats.currentMonthSteps - stats.previousMonthSteps
        if (stats.previousMonthSteps > 0L) {
            val pct = (diff.toDouble() / stats.previousMonthSteps) * 100.0
            StatRow(
                "Variación",
                if (diff >= 0) "+${kmEs(pct, 1)} %" else "${kmEs(pct, 1)} %",
                valueColor = if (diff >= 0) GoldBright else Negative,
            )
        }
    }
}

@Composable
private fun YearlyCard(stats: StatsState) {
    CodexCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("Año ${stats.currentYear}", null, intEs(stats.currentYearSteps))
        Spacer(Modifier.height(14.dp))
        BarChart(
            values = stats.monthlySeries,
            labels = MONTH_LABELS,
            modifier = Modifier.fillMaxWidth().height(170.dp),
            compactLabels = true,
        )
    }
}

@Composable
private fun CaloriesCard(stats: StatsState) {
    CodexCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(CellBg)
                        .border(1.dp, GoldDivider, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = Ember,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Eyebrow("Energía de la travesía")
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Calorías quemadas (estimación)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextFaint,
                    )
                }
            }
            Text(
                "${intEs(stats.journeyCalories)} kcal",
                style = MaterialTheme.typography.titleMedium,
                color = GoldBright,
            )
        }
        Spacer(Modifier.height(10.dp))
        StatRow("Hoy", "${intEs(stats.todayCalories)} kcal")
        StatRow("Esta semana", "${intEs(stats.weekCalories)} kcal")
        StatRow("Este mes", "${intEs(stats.monthCalories)} kcal")
        StatRow("Año ${stats.currentYear}", "${intEs(stats.yearCalories)} kcal")
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = GoldBright) {
    HorizontalDivider(color = GoldDivider)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleSmall, color = valueColor)
    }
}

@Composable
private fun BarChart(
    values: List<Long>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    compactLabels: Boolean = false,
    showValues: Boolean = true,
    peakIndex: Int? = null,
) {
    val emptyBarColor = GoldBase.copy(alpha = 0.12f)
    val axisColor = GoldBase.copy(alpha = 0.14f)
    val labelColor = TextSecondary
    val valueColor = TextBody

    val labelStyle = TextStyle(color = labelColor, fontSize = if (compactLabels) 10.sp else 11.sp, textAlign = TextAlign.Center)
    val valueStyle = TextStyle(color = valueColor, fontSize = 9.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)

    val measurer = rememberTextMeasurer()
    val maxValue = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val autoPeak = peakIndex ?: values.withIndex().filter { it.value > 0L }.maxByOrNull { it.value }?.index

    Canvas(modifier = modifier) {
        drawBarChart(
            values, labels, maxValue, autoPeak,
            emptyBarColor, axisColor, measurer, labelStyle, valueStyle, showValues,
        )
    }
}

private fun DrawScope.drawBarChart(
    values: List<Long>,
    labels: List<String>,
    maxValue: Long,
    peakIndex: Int?,
    emptyBarColor: Color,
    axisColor: Color,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
    valueStyle: TextStyle,
    showValues: Boolean,
) {
    val labelHeight = 18.dp.toPx()
    val valueHeight = 14.dp.toPx()
    val chartTop = valueHeight
    val chartBottom = size.height - labelHeight
    val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
    val n = values.size.coerceAtLeast(1)
    val spacing = 6.dp.toPx()
    val available = size.width - spacing * (n + 1)
    val barWidth = (available / n).coerceAtLeast(2f)

    // Eje base
    drawLine(axisColor, Offset(0f, chartBottom), Offset(size.width, chartBottom), strokeWidth = 1f)

    values.forEachIndexed { index, value ->
        val x = spacing + index * (barWidth + spacing)
        val ratio = value.toFloat() / maxValue.toFloat()
        val h = (chartHeight * ratio).coerceAtLeast(if (value > 0L) 2f else 0f)
        val top = chartBottom - h

        if (value <= 0L) {
            drawRoundRect(emptyBarColor, topLeft = Offset(x, chartBottom - 2f), size = Size(barWidth, 2f))
        } else {
            val isPeak = index == peakIndex
            val brush = Brush.verticalGradient(
                colors = if (isPeak) listOf(BarPeakTop, BarPeakBottom) else listOf(BarNormalTop, BarNormalBottom),
                startY = top,
                endY = chartBottom,
            )
            drawRoundRect(brush = brush, topLeft = Offset(x, top), size = Size(barWidth, h))
            if (showValues) {
                val valueLabel = compactNumber(value)
                val measured = measurer.measure(AnnotatedString(valueLabel), valueStyle)
                val tx = x + (barWidth - measured.size.width) / 2f
                val ty = (top - measured.size.height - 2f).coerceAtLeast(0f)
                drawText(measurer, valueLabel, Offset(tx, ty), valueStyle)
            }
        }

        val label = labels.getOrNull(index).orEmpty()
        if (label.isNotEmpty()) {
            val measured = measurer.measure(AnnotatedString(label), labelStyle)
            val tx = x + (barWidth - measured.size.width) / 2f
            drawText(measurer, label, Offset(tx, chartBottom + 4f), labelStyle)
        }
    }
}

private val HOUR_LABELS = List(24) { if (it % 3 == 0) it.toString() else "" }
private val WEEK_LABELS = listOf("L", "M", "X", "J", "V", "S", "D")
private val DOW_LONG = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val MONTH_LABELS = listOf("E", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
