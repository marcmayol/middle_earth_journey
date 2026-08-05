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
import com.marcm.middleearthjourney.data.DEFAULT_HEIGHT_CM
import com.marcm.middleearthjourney.data.DEFAULT_WEIGHT_KG
import com.marcm.middleearthjourney.data.caloriesBurned
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
import kotlin.math.roundToLong

/** Perfil corporal + preferencia de calorías (de los ajustes). */
private data class Settings(
    val weightKg: Int,
    val heightCm: Int,
    val showCalories: Boolean,
)

/** Entradas del repositorio que alimentan [JourneyState]. */
private data class JourneyInputs(
    val steps: Long,
    val startEpoch: Long?,
    val today: Long,
    val route: RouteId,
    val direction: Direction,
)

/** Entradas del repositorio que alimentan [StatsState]. */
private data class StatsInputs(
    val daily: Map<Long, Long>,
    val hourly: List<Long>,
    val journeySteps: Long,
)

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
    val todayCalories: Long,
    val showCalories: Boolean,
    val routeId: RouteId,
    val direction: Direction,
    val routeTitle: String,
    val destinationName: String,
    val goalLabel: String,
    val arrivedLabel: String,
    val waypoints: List<Waypoint>,
    val canStartReturn: Boolean,
)

/** Una semana natural (lunes a domingo) del historial. */
data class WeekStats(
    val start: LocalDate,
    val steps: List<Long>,
    val total: Long,
    val bestDayOfWeek: DayOfWeek?,
    val bestDaySteps: Long,
)

data class StatsState(
    /** De la más antigua con datos a la actual; la última es siempre la semana en curso. */
    val weeks: List<WeekStats>,
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
    val caloriesEnabled: Boolean,
    val journeyCalories: Long,
    val todayCalories: Long,
    val weekCalories: Long,
    val monthCalories: Long,
    val yearCalories: Long,
) {
    /** La semana en curso: la que se ve al abrir la pantalla. */
    val currentWeek: WeekStats get() = weeks.last()

    companion object {
        val EMPTY = StatsState(
            weeks = listOf(
                WeekStats(
                    start = mondayOf(today()),
                    steps = List(7) { 0L },
                    total = 0L,
                    bestDayOfWeek = null,
                    bestDaySteps = 0L,
                ),
            ),
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
            caloriesEnabled = true,
            journeyCalories = 0L,
            todayCalories = 0L,
            weekCalories = 0L,
            monthCalories = 0L,
            yearCalories = 0L,
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

    /** Perfil corporal + preferencia de calorías, combinados para alimentar los estados. */
    private val settingsFlow: StateFlow<Settings> = combine(
        repo.weightKg,
        repo.heightCm,
        repo.showCalories,
    ) { weight, height, showCalories ->
        Settings(weight, height, showCalories)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Settings(DEFAULT_WEIGHT_KG, DEFAULT_HEIGHT_CM, true),
    )

    private val journeyInputs: StateFlow<JourneyInputs> = combine(
        repo.journeySteps,
        repo.journeyStart,
        repo.todaySteps,
        repo.activeRoute,
        repo.activeDirection,
    ) { steps, startEpoch, today, route, direction ->
        JourneyInputs(steps, startEpoch, today, route, direction)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        JourneyInputs(0L, null, 0L, RouteId.FRODO, Direction.FORWARD),
    )

    val state: StateFlow<JourneyState> = combine(journeyInputs, settingsFlow) { input, settings ->
        buildState(input, settings)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        buildState(
            JourneyInputs(0L, null, 0L, RouteId.FRODO, Direction.FORWARD),
            Settings(DEFAULT_WEIGHT_KG, DEFAULT_HEIGHT_CM, true),
        ),
    )

    private val statsInputs: StateFlow<StatsInputs> = combine(
        repo.dailySteps,
        repo.hourlySteps,
        repo.journeySteps,
    ) { daily, hourly, journey ->
        StatsInputs(daily, hourly, journey)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StatsInputs(emptyMap(), List(24) { 0L }, 0L))

    val stats: StateFlow<StatsState> = combine(statsInputs, settingsFlow) { input, settings ->
        buildStats(input, settings)
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

    // ---- Ajustes del perfil corporal ----
    val weightKg: StateFlow<Int> = repo.weightKg
    val heightCm: StateFlow<Int> = repo.heightCm
    val showCalories: StateFlow<Boolean> = repo.showCalories

    fun setWeightKg(kg: Int) {
        viewModelScope.launch { repo.setWeightKg(kg) }
    }

    fun setHeightCm(cm: Int) {
        viewModelScope.launch { repo.setHeightCm(cm) }
    }

    fun setShowCalories(show: Boolean) {
        viewModelScope.launch { repo.setShowCalories(show) }
    }

    private fun buildState(input: JourneyInputs, settings: Settings): JourneyState {
        val steps = input.steps
        val startEpoch = input.startEpoch
        val todayStepsValue = input.today
        val routeId = input.route
        val direction = input.direction
        val height = settings.heightCm
        val weight = settings.weightKg
        val route = Routes.byId(routeId)
        val waypoints = route.orientedWaypoints(direction)
        val achievements = route.orientedAchievements(direction)
        val km = stepsToKm(steps, height)
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
            todayKm = stepsToKm(todayStepsValue, height),
            todayCalories = caloriesBurned(todayStepsValue, weight, height).roundToLong(),
            showCalories = settings.showCalories,
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

    private fun buildStats(input: StatsInputs, settings: Settings): StatsState {
        val daily = input.daily
        val hourly = input.hourly
        val weight = settings.weightKg
        val height = settings.heightCm
        fun kcal(steps: Long): Long = caloriesBurned(steps, weight, height).roundToLong()
        val now = today()
        val currentWeekStart = mondayOf(now)

        // Historial semanal: desde la semana del primer día con datos hasta la actual.
        val firstDataDay = daily.entries.filter { it.value > 0L }.minOfOrNull { it.key }
        val oldestWeekStart = firstDataDay
            ?.let { mondayOf(dateFromEpochDay(it)) }
            ?.takeIf { it.epochDay() < currentWeekStart.epochDay() }
            ?: currentWeekStart
        val weeksBack = ((currentWeekStart.epochDay() - oldestWeekStart.epochDay()) / 7)
            .toInt()
            .coerceIn(0, MAX_WEEKS_BACK)
        val weeks = (weeksBack downTo 0).map { back ->
            weekStatsAt(daily, plusDays(currentWeekStart, -7L * back))
        }
        val weeklyTotal = weeks.last().total

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
        val firstDay = firstDataDay?.let { dateFromEpochDay(it) }
        val monthLabel = MONTH_NAMES.getOrElse(now.monthNumber - 1) { "" }

        return StatsState(
            weeks = weeks,
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
            caloriesEnabled = settings.showCalories,
            journeyCalories = kcal(input.journeySteps),
            todayCalories = kcal(daily[now.epochDay()] ?: 0L),
            weekCalories = kcal(weeklyTotal),
            monthCalories = kcal(currentMonthSteps),
            yearCalories = kcal(currentYearSteps),
        )
    }

    /** Resumen de la semana que empieza en [weekStart] (lunes). */
    private fun weekStatsAt(daily: Map<Long, Long>, weekStart: LocalDate): WeekStats {
        val steps = (0..6).map { offset ->
            daily[plusDays(weekStart, offset.toLong()).epochDay()] ?: 0L
        }
        val bestIdx = steps.withIndex().filter { it.value > 0L }.maxByOrNull { it.value }?.index
        return WeekStats(
            start = weekStart,
            steps = steps,
            total = steps.sum(),
            bestDayOfWeek = bestIdx?.let { dayOfWeekFromMondayIndex(it) },
            bestDaySteps = if (bestIdx != null) steps[bestIdx] else 0L,
        )
    }

    companion object {
        /** Tope de semanas navegables hacia atrás (2 años, lo que guarda el historial diario). */
        private const val MAX_WEEKS_BACK = 104

        private val MONTH_NAMES = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
        )
    }
}
