package com.marcm.middleearthjourney.data

import com.marcm.middleearthjourney.util.currentHour
import com.marcm.middleearthjourney.util.epochDay
import com.marcm.middleearthjourney.util.today
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import platform.CoreMotion.CMPedometer
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate

/**
 * Implementación iOS de [JourneyRepository]: pasos con CMPedometer (CoreMotion) y persistencia
 * con multiplatform-settings (NSUserDefaults). Replica la lógica de la versión Android
 * (StepRepository) usando como fuente los pasos de HOY que da el podómetro del sistema.
 *
 * ⚠️ Para tu amigo: revisar en el dispositivo. Puntos a verificar:
 *  - permiso de movimiento (Info.plist `NSMotionUsageDescription`).
 *  - CMPedometer.isStepCountingAvailable().
 *  - el muestreo (poll cada 3 s) es sencillo; se puede mejorar con startPedometerUpdates.
 */
@OptIn(ExperimentalForeignApi::class)
class IosJourneyRepository(private val settings: Settings) : JourneyRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val pedometer = CMPedometer()

    // Estado en memoria (persistido en Settings)
    private var accumulated: Long = 0L            // pasos totales históricos (cierre de días anteriores)
    private var todayBaselineCount: Long = 0L     // pasos de hoy ya contabilizados en accumulated
    private var todayEpochStored: Long = -1L
    private var missionStartEpochDay: Long = -1L
    private var journeyBaseline: Long = 0L
    private var journeyStartEpochDay: Long = -1L
    private var routeChosenValue: Boolean = true
    private var activeRouteId: RouteId = RouteId.FRODO
    private var activeDirectionValue: Direction = Direction.FORWARD
    private val dailyHistory = HashMap<Long, Long>()
    private val hourly = LongArray(24)
    private var hourlyEpochDay: Long = -1L
    private var cineSeenValue: Set<Int> = emptySet()
    private var eventSeenValue: List<String> = emptyList()
    private var eventCheckpointValue: Int = 0
    private var eventPendingValue: String? = null

    private val _journeySteps = MutableStateFlow(0L)
    override val journeySteps: StateFlow<Long> = _journeySteps
    private val _journeyStart = MutableStateFlow<Long?>(null)
    override val journeyStart: StateFlow<Long?> = _journeyStart
    private val _todaySteps = MutableStateFlow(0L)
    override val todaySteps: StateFlow<Long> = _todaySteps
    private val _dailySteps = MutableStateFlow<Map<Long, Long>>(emptyMap())
    override val dailySteps: StateFlow<Map<Long, Long>> = _dailySteps
    private val _hourlySteps = MutableStateFlow<List<Long>>(List(24) { 0L })
    override val hourlySteps: StateFlow<List<Long>> = _hourlySteps
    private val _activeRoute = MutableStateFlow(RouteId.FRODO)
    override val activeRoute: StateFlow<RouteId> = _activeRoute
    private val _activeDirection = MutableStateFlow(Direction.FORWARD)
    override val activeDirection: StateFlow<Direction> = _activeDirection
    private val _routeChosen = MutableStateFlow(true)
    override val routeChosen: StateFlow<Boolean> = _routeChosen
    private val _cineSeen = MutableStateFlow<Set<Int>>(emptySet())
    override val cineSeen: StateFlow<Set<Int>> = _cineSeen
    private val _eventSeen = MutableStateFlow<List<String>>(emptyList())
    override val eventSeen: StateFlow<List<String>> = _eventSeen
    private val _eventCheckpoint = MutableStateFlow(0)
    override val eventCheckpoint: StateFlow<Int> = _eventCheckpoint
    private val _eventPending = MutableStateFlow<String?>(null)
    override val eventPending: StateFlow<String?> = _eventPending

    override val hasSensor: Boolean get() = CMPedometer.isStepCountingAvailable()

    @Volatile private var loaded = false

    init {
        load()
        startPolling()
    }

    private fun load() {
        accumulated = settings["accumulated_steps", 0L]
        todayBaselineCount = settings["ios_today_baseline", 0L]
        todayEpochStored = settings["ios_today_epoch", -1L]
        missionStartEpochDay = settings["mission_start_epoch_day", -1L]
        journeyBaseline = settings["journey_baseline_steps", 0L]
        journeyStartEpochDay = settings["journey_start_epoch_day", -1L]
        routeChosenValue = settings["route_chosen", true]
        activeRouteId = runCatching { RouteId.valueOf(settings["active_route", "FRODO"]) }.getOrDefault(RouteId.FRODO)
        activeDirectionValue = runCatching { Direction.valueOf(settings["active_direction", "FORWARD"]) }.getOrDefault(Direction.FORWARD)
        dailyHistory.clear(); dailyHistory.putAll(deserializeDaily(settings["daily_history", ""]))
        cineSeenValue = settings["cine_seen_chapters", ""].split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        eventSeenValue = settings["event_seen", ""].split(",").filter { it.isNotBlank() }
        eventCheckpointValue = settings["event_checkpoint", 0L].toInt()
        eventPendingValue = settings["event_pending", ""].ifBlank { null }
        publishAll()
    }

    private fun publishAll() {
        _activeRoute.value = activeRouteId
        _activeDirection.value = activeDirectionValue
        _routeChosen.value = routeChosenValue
        _journeyStart.value = if (journeyStartEpochDay >= 0L) journeyStartEpochDay else null
        _cineSeen.value = cineSeenValue
        _eventSeen.value = eventSeenValue
        _eventCheckpoint.value = eventCheckpointValue
        _eventPending.value = eventPendingValue
        _dailySteps.value = HashMap(dailyHistory)
        _journeySteps.value = (accumulated - journeyBaseline).coerceAtLeast(0L)
        _todaySteps.value = dailyHistory[today().epochDay()] ?: 0L
        _hourlySteps.value = hourly.toList()
    }

    /** Muestrea CMPedometer (pasos de HOY) y acumula deltas, como hace Android con el sensor. */
    private fun startPolling() {
        scope.launch {
            while (true) {
                queryTodaySteps { todayCount -> onTodayCount(todayCount) }
                delay(3000)
            }
        }
    }

    private fun onTodayCount(todayCount: Long) {
        val todayEpoch = today().epochDay()
        if (todayEpochStored != todayEpoch) {
            // Cambio de día: lo de hoy se vuelve base nueva.
            todayEpochStored = todayEpoch
            todayBaselineCount = 0L
            if (hourlyEpochDay != todayEpoch) { hourly.fill(0L); hourlyEpochDay = todayEpoch }
        }
        val delta = (todayCount - todayBaselineCount).coerceAtLeast(0L)
        if (delta > 0L) {
            accumulated += delta
            if (missionStartEpochDay < 0L) missionStartEpochDay = todayEpoch
            if (journeyStartEpochDay < 0L) journeyStartEpochDay = todayEpoch
            dailyHistory[todayEpoch] = (dailyHistory[todayEpoch] ?: 0L) + delta
            val h = currentHour().coerceIn(0, 23)
            if (hourlyEpochDay != todayEpoch) { hourly.fill(0L); hourlyEpochDay = todayEpoch }
            hourly[h] += delta
            todayBaselineCount = todayCount
            persist()
            publishAll()
        }
    }

    /** Consulta los pasos de hoy (desde las 00:00) con CMPedometer. */
    private fun queryTodaySteps(cb: (Long) -> Unit) {
        if (!CMPedometer.isStepCountingAvailable()) return
        val startOfDay = NSCalendar.currentCalendar.startOfDayForDate(NSDate())
        pedometer.queryPedometerDataFromDate(startOfDay, toDate = NSDate()) { data, _ ->
            val steps = data?.numberOfSteps?.longValue ?: 0L
            scope.launch { cb(steps) }
        }
    }

    override suspend fun ensureLoaded() { /* cargado en init */ }

    override suspend fun startJourney(route: RouteId, direction: Direction) {
        activeRouteId = route
        activeDirectionValue = direction
        journeyBaseline = accumulated
        journeyStartEpochDay = today().epochDay()
        routeChosenValue = true
        cineSeenValue = emptySet(); eventSeenValue = emptyList(); eventCheckpointValue = 0; eventPendingValue = null
        persist(); publishAll()
    }

    override suspend fun markCineSeen(indices: Set<Int>) {
        if (indices.isEmpty()) return
        cineSeenValue = cineSeenValue + indices
        persist(); _cineSeen.value = cineSeenValue
    }

    override suspend fun applyEventResolution(checkpoint: Int, seen: List<String>, pending: String?) {
        eventCheckpointValue = checkpoint; eventSeenValue = seen; eventPendingValue = pending
        persist()
        _eventCheckpoint.value = checkpoint; _eventSeen.value = seen; _eventPending.value = pending
    }

    override suspend fun clearEventPending() {
        eventPendingValue = null; persist(); _eventPending.value = null
    }

    private fun persist() {
        settings["accumulated_steps"] = accumulated
        settings["ios_today_baseline"] = todayBaselineCount
        settings["ios_today_epoch"] = todayEpochStored
        settings["mission_start_epoch_day"] = missionStartEpochDay
        settings["journey_baseline_steps"] = journeyBaseline
        settings["journey_start_epoch_day"] = journeyStartEpochDay
        settings["route_chosen"] = routeChosenValue
        settings["active_route"] = activeRouteId.name
        settings["active_direction"] = activeDirectionValue.name
        settings["daily_history"] = serializeDaily(dailyHistory)
        settings["cine_seen_chapters"] = cineSeenValue.joinToString(",")
        settings["event_seen"] = eventSeenValue.joinToString(",")
        settings["event_checkpoint"] = eventCheckpointValue.toLong()
        settings["event_pending"] = eventPendingValue ?: ""
    }

    private fun serializeDaily(map: Map<Long, Long>): String =
        map.entries.joinToString(";") { "${it.key}:${it.value}" }

    private fun deserializeDaily(raw: String): Map<Long, Long> {
        if (raw.isBlank()) return emptyMap()
        val out = HashMap<Long, Long>()
        for (entry in raw.split(';')) {
            val c = entry.indexOf(':'); if (c <= 0) continue
            val d = entry.substring(0, c).toLongOrNull() ?: continue
            val n = entry.substring(c + 1).toLongOrNull() ?: continue
            out[d] = n
        }
        return out
    }
}
