package com.marcm.middleearthjourney.util

import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

// ───────────────────────── Números (formato español) ─────────────────────────

/** Entero con punto de millar: 13873 → "13.873". */
fun intEs(v: Long): String {
    val neg = v < 0
    val s = abs(v).toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
        sb.append(s[i])
    }
    return if (neg) "-$sb" else sb.toString()
}

/** Decimal con coma y punto de millar: 2860.5 → "2.860,5". */
fun kmEs(v: Double, decimals: Int = 1): String {
    val neg = v < 0
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val rounded = (abs(v) * factor).roundToLong()
    val intPart = rounded / factor
    val fracPart = rounded % factor
    val intStr = intEs(intPart)
    val out = if (decimals > 0) "$intStr,${fracPart.toString().padStart(decimals, '0')}" else intStr
    return if (neg) "-$out" else out
}

/** Un decimal con coma, con signo opcional: 12.34 → "12,3". */
fun decimal1(v: Double): String = kmEs(v, 1)

/** Versión compacta para gráficas (mantiene punto, estilo "13k"). */
fun compactNumber(value: Long): String = when {
    value >= 1_000_000L -> oneDecimalDot(value / 1_000_000.0) + "M"
    value >= 10_000L -> (value / 1_000.0).roundToLong().let { it.toString() } + "k"
    value >= 1_000L -> oneDecimalDot(value / 1_000.0) + "k"
    else -> value.toString()
}

/** Un decimal con PUNTO (para etiquetas tipo "1.3k"): 1.34 → "1.3". */
fun oneDecimalDot(v: Double): String {
    val r = (abs(v) * 10).roundToLong()
    val sign = if (v < 0) "-" else ""
    return "$sign${r / 10}.${r % 10}"
}

/** Entero a 2 cifras: 5 → "05". */
fun pad2(v: Int): String = v.toString().padStart(2, '0')

// ───────────────────────── Fechas (formato español) ─────────────────────────

private val MONTHS_SHORT = listOf(
    "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
)
private val MONTHS_LONG = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

/** "13 may" */
fun fmtDayMonth(d: LocalDate): String = "${d.dayOfMonth} ${MONTHS_SHORT[d.monthNumber - 1]}"

/** "13 may 2026" */
fun fmtDayMonthYear(d: LocalDate): String =
    "${d.dayOfMonth} ${MONTHS_SHORT[d.monthNumber - 1]} ${d.year}"

/** "13 de mayo de 2026" */
fun fmtLongDate(d: LocalDate): String =
    "${d.dayOfMonth} de ${MONTHS_LONG[d.monthNumber - 1]} de ${d.year}"

/** Nombre largo del mes (1-12). */
fun monthNameLong(month1to12: Int): String = MONTHS_LONG[month1to12 - 1].replaceFirstChar { it.uppercase() }
