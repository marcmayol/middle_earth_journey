package com.marcm.middleearthjourney

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcm.middleearthjourney.data.Achievement
import com.marcm.middleearthjourney.data.Achievements
import com.marcm.middleearthjourney.data.Direction
import com.marcm.middleearthjourney.data.JourneyEvent
import com.marcm.middleearthjourney.data.JourneyEvents
import com.marcm.middleearthjourney.data.JourneyRepository
import com.marcm.middleearthjourney.data.Quotes
import com.marcm.middleearthjourney.data.RouteId
import com.marcm.middleearthjourney.data.Routes
import com.marcm.middleearthjourney.data.Waypoint
import com.marcm.middleearthjourney.data.stepsToKm
import com.marcm.middleearthjourney.util.dateFromEpochDay
import com.marcm.middleearthjourney.util.dayOfWeekFromMondayIndex
import com.marcm.middleearthjourney.util.daysInMonth
import com.marcm.middleearthjourney.util.epochDay
import com.marcm.middleearthjourney.util.firstOfMonth
import com.marcm.middleearthjourney.util.minusMonths
import com.marcm.middleearthjourney.util.mondayOf
import com.marcm.middleearthjourney.util.plusDays
import com.marcm.middleearthjourney.util.today
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

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
            weekStart = mondayOf(today()),
            weeklySteps = List(7) { 0L },
            weeklyTotal = 0L,
            bestDayOfWeek = null,
            bestDaySteps = 0L,
            monthLabel = "",
            currentMonthSteps = 0L,
            previousMonthSteps = 0L,
            avgStepsPerDayThisMonth = 0L,
            currentYear = today().year,
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

class MainViewModel(private val repo: JourneyRepository) : ViewModel() {

    val hasSensor: Boolean get() = repo.hasSensor

    val quoteOfTheDay: String = Quotes.forDay(today().epochDay())

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

        val missionStart = startEpoch?.let { dateFromEpochDay(it) }
        val now = today()
        val daysElapsed = missionStart?.let {
            (now.epochDay() - it.epochDay()).toInt().coerceAtLeast(0) + 1
        } ?: 0
        val avgKmPerDay = if (daysElapsed > 0) km / daysElapsed else 0.0
        val estimatedArrival = if (avgKmPerDay > 0.05 && !finished) {
            val remaining = total - km
            val daysToGo = (remaining / avgKmPerDay).toLong()
            plusDays(now, daysToGo)
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
        val now = today()
        val weekStart = mondayOf(now)
        val weekly = (0..6).map { offset ->
            daily[plusDays(weekStart, offset.toLong()).epochDay()] ?: 0L
        }
        val weeklyTotal = weekly.sum()
        val bestIdx = weekly.withIndex().filter { it.value > 0L }.maxByOrNull { it.value }?.index
        val bestDow = bestIdx?.let { dayOfWeekFromMondayIndex(it) }
        val bestSteps = if (bestIdx != null) weekly[bestIdx] else 0L

        val firstThisMonth = firstOfMonth(now)
        val daysThisMonth = daysInMonth(now.year, now.monthNumber)
        var currentMonthSteps = 0L
        for (d in 0 until daysThisMonth) {
            currentMonthSteps += daily[plusDays(firstThisMonth, d.toLong()).epochDay()] ?: 0L
        }
        val prevFirst = minusMonths(firstThisMonth, 1)
        val prevDays = daysInMonth(prevFirst.year, prevFirst.monthNumber)
        var previousMonthSteps = 0L
        for (d in 0 until prevDays) {
            previousMonthSteps += daily[plusDays(prevFirst, d.toLong()).epochDay()] ?: 0L
        }

        val avgPerDay = if (now.dayOfMonth > 0) currentMonthSteps / now.dayOfMonth else 0L

        val year = now.year
        val monthlySeries = (1..12).map { monthIdx ->
            val first = LocalDate(year, monthIdx, 1)
            val len = daysInMonth(year, monthIdx)
            var sum = 0L
            for (d in 0 until len) {
                sum += daily[plusDays(first, d.toLong()).epochDay()] ?: 0L
            }
            sum
        }
        val currentYearSteps = monthlySeries.sum()

        val daysWithData = daily.count { it.value > 0L }
        val firstDay = daily.entries
            .filter { it.value > 0L }
            .minByOrNull { it.key }
            ?.let { dateFromEpochDay(it.key) }
        val monthLabel = MONTH_NAMES.getOrElse(now.monthNumber - 1) { "" }

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
            todaySteps = daily[now.epochDay()] ?: 0L,
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
