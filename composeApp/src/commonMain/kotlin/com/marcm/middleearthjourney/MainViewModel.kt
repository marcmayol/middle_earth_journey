package com.marcm.middleearthjourney

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marcm.middleearthjourney.data.Achievement
import com.marcm.middleearthjourney.data.Achievements
import com.marcm.middleearthjourney.data.Direction
import com.marcm.middleearthjourney.data.JourneyEvent
import com.marcm.middleearthjourney.data.JourneyEvents
import com.marcm.middleearthjourney.data.Quotes
import com.marcm.middleearthjourney.data.RouteId
import com.marcm.middleearthjourney.data.Routes
import com.marcm.middleearthjourney.data.StepRepository
import com.marcm.middleearthjourney.data.Waypoint
import com.marcm.middleearthjourney.data.stepsToKm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class JourneyState(
    val steps: Long,
    val km: Double,
    val totalKm: Double,
    val progress: Float,
    val currentIndex: Int,
    val currentWaypoint: Waypoint,
    val nextWaypoint: Waypoint?,
    val kmToNext: Double,
    val legProgress: Float,
    val legTotalKm: Double,
    val finished: Boolean,
    val unlocked: List<Achievement>,
    val nextAchievement: Achievement?,
    val totalAchievements: Int,
    val allAchievements: List<Achievement>,
    val missionStart: LocalDate?,
    val estimatedArrival: LocalDate?,
    val daysElapsed: Int,
    val avgKmPerDay: Double,
    val todaySteps: Long,
    val todayKm: Double,
    val routeId: RouteId,
    val direction: Direction,
    val routeTitle: String,
    val destinationName: String,
    val goalLabel: String,
    val arrivedLabel: String,
    val waypoints: List<Waypoint>,
    val canStartReturn: Boolean,
)

data class StatsState(
    val weekStart: LocalDate,
    val weeklySteps: List<Long>,
    val weeklyTotal: Long,
    val bestDayOfWeek: DayOfWeek?,
    val bestDaySteps: Long,
    val monthLabel: String,
    val currentMonthSteps: Long,
    val previousMonthSteps: Long,
    val avgStepsPerDayThisMonth: Long,
    val currentYear: Int,
    val currentYearSteps: Long,
    val monthlySeries: List<Long>,
    val daysWithData: Int,
    val hasHistory: Boolean,
    val firstDayWithData: LocalDate?,
    val todaySteps: Long,
    val hourlySteps: List<Long>,
) {
    companion object {
        val EMPTY = StatsState(
            weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
            weeklySteps = List(7) { 0L },
            weeklyTotal = 0L,
            bestDayOfWeek = null,
            bestDaySteps = 0L,
            monthLabel = "",
            currentMonthSteps = 0L,
            previousMonthSteps = 0L,
            avgStepsPerDayThisMonth = 0L,
            currentYear = LocalDate.now().year,
            currentYearSteps = 0L,
            monthlySeries = List(12) { 0L },
            daysWithData = 0,
            hasHistory = false,
            firstDayWithData = null,
            todaySteps = 0L,
            hourlySteps = List(24) { 0L },
        )
    }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo: StepRepository = (app as MiddleEarthApp).stepRepository

    val hasSensor: Boolean get() = repo.hasSensor

    val quoteOfTheDay: String = Quotes.forDay(LocalDate.now().toEpochDay())

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted

    /** false solo en el primer arranque de una instalación nueva (aún sin ruta elegida). */
    val routeChosen: StateFlow<Boolean> = repo.routeChosen

    /** Índices de capítulo de cinemática ya auto-reproducidos en el viaje activo. */
    val cineSeen: StateFlow<Set<Int>> = repo.cineSeen

    fun markCinematicsSeen(indices: Set<Int>) {
        viewModelScope.launch { repo.markCineSeen(indices) }
    }

    val state: StateFlow<JourneyState> = combine(
        repo.journeySteps,
        repo.journeyStart,
        repo.todaySteps,
        repo.activeRoute,
        repo.activeDirection,
    ) { steps, startEpoch, today, route, direction ->
        buildState(steps, startEpoch, today, route, direction)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        buildState(0L, null, 0L, RouteId.FRODO, Direction.FORWARD),
    )

    val stats: StateFlow<StatsState> = combine(
        repo.dailySteps,
        repo.hourlySteps,
    ) { daily, hourly ->
        buildStats(daily, hourly)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsState.EMPTY)

    /** Suceso aleatorio pendiente de mostrar (popup), o null. */
    val pendingEvent: StateFlow<JourneyEvent?> = combine(
        repo.eventPending,
        repo.activeRoute,
    ) { id, route -> id?.let { JourneyEvents.byId(route, it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Diario: sucesos ya vividos en el viaje activo, del más reciente al más antiguo. */
    val eventLog: StateFlow<List<JourneyEvent>> = combine(
        repo.eventSeen,
        repo.activeRoute,
    ) { seen, route -> seen.mapNotNull { JourneyEvents.byId(route, it) }.reversed() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { repo.ensureLoaded() }
    }

    fun dismissEvent() {
        viewModelScope.launch { repo.clearEventPending() }
    }

    private fun buildState(
        steps: Long,
        startEpoch: Long?,
        todayStepsValue: Long,
        routeId: RouteId,
        direction: Direction,
    ): JourneyState {
        val route = Routes.byId(routeId)
        val waypoints = route.orientedWaypoints(direction)
        val achievements = route.orientedAchievements(direction)
        val km = stepsToKm(steps)
        val total = route.totalKm
        val progress = (km / total).toFloat().coerceIn(0f, 1f)
        val idx = Routes.currentWaypointIndex(waypoints, km)
        val current = waypoints[idx]
        val next = waypoints.getOrNull(idx + 1)
        val legFrom = current.distanceKm
        val legTo = next?.distanceKm ?: total
        val legTotalKm = (legTo - legFrom).coerceAtLeast(0.0001)
        val kmToNext = legTo - km
        val legProgress = ((km - legFrom) / legTotalKm).toFloat().coerceIn(0f, 1f)
        val finished = km >= total

        val missionStart = startEpoch?.let { LocalDate.ofEpochDay(it) }
        val today = LocalDate.now()
        val daysElapsed = missionStart?.let {
            (today.toEpochDay() - it.toEpochDay()).toInt().coerceAtLeast(0) + 1
        } ?: 0
        val avgKmPerDay = if (daysElapsed > 0) km / daysElapsed else 0.0
        val estimatedArrival = if (avgKmPerDay > 0.05 && !finished) {
            val remaining = total - km
            val daysToGo = (remaining / avgKmPerDay).toLong()
            today.plusDays(daysToGo)
        } else null

        val destinationName = route.destinationName(direction)
        val goalLabel = when (direction) {
            Direction.FORWARD -> "Hacia $destinationName"
            Direction.RETURN -> "De vuelta a $destinationName"
        }
        val arrivedLabel = when (direction) {
            Direction.FORWARD -> "Has llegado a $destinationName"
            Direction.RETURN -> "Has vuelto a casa: $destinationName"
        }

        return JourneyState(
            steps = steps,
            km = km,
            totalKm = total,
            progress = progress,
            currentIndex = idx,
            currentWaypoint = current,
            nextWaypoint = next,
            kmToNext = kmToNext.coerceAtLeast(0.0),
            legProgress = legProgress,
            legTotalKm = legTotalKm,
            finished = finished,
            unlocked = Achievements.unlocked(achievements, km),
            nextAchievement = Achievements.nextLocked(achievements, km),
            totalAchievements = achievements.size,
            allAchievements = achievements,
            missionStart = missionStart,
            estimatedArrival = estimatedArrival,
            daysElapsed = daysElapsed,
            avgKmPerDay = avgKmPerDay,
            todaySteps = todayStepsValue,
            todayKm = stepsToKm(todayStepsValue),
            routeId = routeId,
            direction = direction,
            routeTitle = route.title,
            destinationName = destinationName,
            goalLabel = goalLabel,
            arrivedLabel = arrivedLabel,
            waypoints = waypoints,
            canStartReturn = finished && direction == Direction.FORWARD,
        )
    }

    fun onPermissionGranted() {
        _permissionGranted.value = true
    }

    /** Cambia de ruta (en sentido de ida). Reinicia el progreso del viaje actual. */
    fun selectRoute(route: RouteId, direction: Direction = Direction.FORWARD) {
        viewModelScope.launch { repo.startJourney(route, direction) }
    }

    /** Inicia el viaje de vuelta a casa por la ruta activa, en sentido inverso. */
    fun startReturnJourney() {
        viewModelScope.launch {
            repo.startJourney(repo.activeRoute.value, Direction.RETURN)
        }
    }

    private fun buildStats(daily: Map<Long, Long>, hourly: List<Long>): StatsState {
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekly = (0..6).map { offset ->
            daily[weekStart.plusDays(offset.toLong()).toEpochDay()] ?: 0L
        }
        val weeklyTotal = weekly.sum()
        val bestIdx = weekly.withIndex().filter { it.value > 0L }.maxByOrNull { it.value }?.index
        val bestDow = bestIdx?.let { DayOfWeek.of(it + 1) }
        val bestSteps = if (bestIdx != null) weekly[bestIdx] else 0L

        val ym = YearMonth.from(today)
        val firstOfMonth = ym.atDay(1)
        val daysInMonth = ym.lengthOfMonth()
        var currentMonthSteps = 0L
        for (d in 0 until daysInMonth) {
            currentMonthSteps += daily[firstOfMonth.plusDays(d.toLong()).toEpochDay()] ?: 0L
        }
        val prevYm = ym.minusMonths(1)
        val prevFirst = prevYm.atDay(1)
        val prevDays = prevYm.lengthOfMonth()
        var previousMonthSteps = 0L
        for (d in 0 until prevDays) {
            previousMonthSteps += daily[prevFirst.plusDays(d.toLong()).toEpochDay()] ?: 0L
        }

        val avgPerDay = if (today.dayOfMonth > 0) currentMonthSteps / today.dayOfMonth else 0L

        val year = today.year
        val monthlySeries = (1..12).map { monthIdx ->
            val mYm = YearMonth.of(year, monthIdx)
            val first = mYm.atDay(1)
            val len = mYm.lengthOfMonth()
            var sum = 0L
            for (d in 0 until len) {
                sum += daily[first.plusDays(d.toLong()).toEpochDay()] ?: 0L
            }
            sum
        }
        val currentYearSteps = monthlySeries.sum()

        val daysWithData = daily.count { it.value > 0L }
        val firstDay = daily.entries
            .filter { it.value > 0L }
            .minByOrNull { it.key }
            ?.let { LocalDate.ofEpochDay(it.key) }
        val monthLabel = MONTH_NAMES.getOrElse(ym.monthValue - 1) { "" }

        return StatsState(
            weekStart = weekStart,
            weeklySteps = weekly,
            weeklyTotal = weeklyTotal,
            bestDayOfWeek = bestDow,
            bestDaySteps = bestSteps,
            monthLabel = monthLabel,
            currentMonthSteps = currentMonthSteps,
            previousMonthSteps = previousMonthSteps,
            avgStepsPerDayThisMonth = avgPerDay,
            currentYear = year,
            currentYearSteps = currentYearSteps,
            monthlySeries = monthlySeries,
            daysWithData = daysWithData,
            hasHistory = daysWithData > 0,
            firstDayWithData = firstDay,
            todaySteps = daily[today.toEpochDay()] ?: 0L,
            hourlySteps = hourly,
        )
    }

    companion object {
        private val MONTH_NAMES = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
        )
    }
}
