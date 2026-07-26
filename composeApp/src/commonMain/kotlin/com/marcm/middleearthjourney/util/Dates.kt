package com.marcm.middleearthjourney.util

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

private val TZ get() = TimeZone.currentSystemDefault()

fun today(): LocalDate = Clock.System.todayIn(TZ)

fun currentHour(): Int = Clock.System.now().toLocalDateTime(TZ).hour

/** Día epoch como Long (compatibilidad con el almacenamiento). */
fun LocalDate.epochDay(): Long = this.toEpochDays().toLong()

fun dateFromEpochDay(epoch: Long): LocalDate = LocalDate.fromEpochDays(epoch.toInt())

/** Lunes de la semana de [d]. */
fun mondayOf(d: LocalDate): LocalDate = d.minus(DatePeriod(days = d.dayOfWeek.isoDayNumber - 1))

fun firstOfMonth(d: LocalDate): LocalDate = LocalDate(d.year, d.monthNumber, 1)

fun daysInMonth(year: Int, month: Int): Int {
    val first = LocalDate(year, month, 1)
    return first.daysUntil(first.plus(DatePeriod(months = 1)))
}

fun plusDays(d: LocalDate, days: Long): LocalDate = d.plus(DatePeriod(days = days.toInt()))

fun plusMonths(d: LocalDate, months: Int): LocalDate = d.plus(DatePeriod(months = months))

fun minusMonths(d: LocalDate, months: Int): LocalDate = d.minus(DatePeriod(months = months))

/** DayOfWeek desde índice 0..6 (0 = lunes). */
fun dayOfWeekFromMondayIndex(i: Int): DayOfWeek = DayOfWeek.entries[i]

/** Índice 0..6 (0 = lunes) de un DayOfWeek. */
fun DayOfWeek.mondayIndex(): Int = this.isoDayNumber - 1
