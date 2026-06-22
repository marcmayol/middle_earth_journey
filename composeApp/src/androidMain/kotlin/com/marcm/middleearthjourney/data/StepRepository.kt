package com.marcm.middleearthjourney.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "frodo_steps")

private object Keys {
    val ACCUMULATED = longPreferencesKey("accumulated_steps")
    val LAST_SENSOR = longPreferencesKey("last_sensor_value")
    val MISSION_START = longPreferencesKey("mission_start_epoch_day")
    val DAILY_HISTORY = stringPreferencesKey("daily_history")
    val MIGRATED_DAILY_V1 = booleanPreferencesKey("migrated_daily_v1")
    val HOURLY_TODAY = stringPreferencesKey("hourly_today")
    val ACTIVE_ROUTE = stringPreferencesKey("active_route")
    val ACTIVE_DIRECTION = stringPreferencesKey("active_direction")
    val JOURNEY_BASELINE = longPreferencesKey("journey_baseline_steps")
    val JOURNEY_START = longPreferencesKey("journey_start_epoch_day")
    val ROUTE_CHOSEN = booleanPreferencesKey("route_chosen")
    val CINE_SEEN = stringPreferencesKey("cine_seen_chapters")
    val EVENT_SEEN = stringPreferencesKey("event_seen")
    val EVENT_CHECKPOINT = longPreferencesKey("event_checkpoint")
    val EVENT_PENDING = stringPreferencesKey("event_pending")
    val HC_ENABLED = booleanPreferencesKey("hc_enabled")
    val HC_ANCHOR_MILLIS = longPreferencesKey("hc_anchor_millis")
    val HC_ANCHOR_OFFSET = longPreferencesKey("hc_anchor_offset")
}

private const val DAILY_HISTORY_MAX_DAYS = 730L

/** Cuántos días hacia atrás refrescamos el histórico diario desde Health Connect. */
private const val HC_DAILY_WINDOW_DAYS = 120L

class StepRepository(private val appContext: Context) : SensorEventListener, JourneyRepository {

    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val healthConnect = HealthConnectStepSource(appContext)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _totalSteps = MutableStateFlow(0L)
    val totalSteps: StateFlow<Long> = _totalSteps

    override val hasSensor: Boolean get() = stepCounterSensor != null

    @Volatile private var accumulated: Long = 0L
    @Volatile private var lastSensorValue: Long = -1L
    @Volatile private var missionStartEpochDay: Long = -1L
    @Volatile private var loaded: Boolean = false

    private val _missionStart = MutableStateFlow<Long?>(null)
    val missionStart: StateFlow<Long?> = _missionStart

    // ---- Viaje activo (ruta + sentido) ----
    @Volatile private var activeRouteId: RouteId = RouteId.FRODO
    @Volatile private var activeDirectionValue: Direction = Direction.FORWARD
    @Volatile private var journeyBaseline: Long = 0L
    @Volatile private var journeyStartEpochDay: Long = -1L

    private val _activeRoute = MutableStateFlow(RouteId.FRODO)
    override val activeRoute: StateFlow<RouteId> = _activeRoute

    private val _activeDirection = MutableStateFlow(Direction.FORWARD)
    override val activeDirection: StateFlow<Direction> = _activeDirection

    private val _journeySteps = MutableStateFlow(0L)
    override val journeySteps: StateFlow<Long> = _journeySteps

    private val _journeyStart = MutableStateFlow<Long?>(null)
    override val journeyStart: StateFlow<Long?> = _journeyStart

    @Volatile private var routeChosenValue: Boolean = true

    /** false solo en una instalación nueva que aún no ha elegido ruta (primer arranque). */
    private val _routeChosen = MutableStateFlow(true)
    override val routeChosen: StateFlow<Boolean> = _routeChosen

    // ---- Cinemáticas ya auto-reproducidas en el viaje activo (índices de capítulo) ----
    @Volatile private var cineSeenValue: Set<Int> = emptySet()
    private val _cineSeen = MutableStateFlow<Set<Int>>(emptySet())
    override val cineSeen: StateFlow<Set<Int>> = _cineSeen

    // ---- Sucesos aleatorios del viaje activo (ids en orden), checkpoint y pendiente ----
    @Volatile private var eventSeenValue: List<String> = emptyList()
    private val _eventSeen = MutableStateFlow<List<String>>(emptyList())
    override val eventSeen: StateFlow<List<String>> = _eventSeen

    @Volatile private var eventCheckpointValue: Int = 0
    private val _eventCheckpoint = MutableStateFlow(0)
    override val eventCheckpoint: StateFlow<Int> = _eventCheckpoint

    @Volatile private var eventPendingValue: String? = null
    private val _eventPending = MutableStateFlow<String?>(null)
    override val eventPending: StateFlow<String?> = _eventPending

    // ---- Modo Health Connect (fuente agregada: móvil + smartwatch) ----
    // Cuando está activo, los pasos del viaje se cuentan desde un "anchor" (instante de
    // referencia) más un offset que preserva el progreso que ya había al activarlo. Así el
    // cambio de fuente nunca hace retroceder el mapa, y a partir de ahí cuentan también los
    // pasos del reloj. El sensor del móvil queda en reserva (caída automática si se revoca).
    @Volatile private var hcEnabled: Boolean = false
    @Volatile private var hcAnchorMillis: Long = -1L
    @Volatile private var hcAnchorOffset: Long = 0L

    private val _usingHealthConnect = MutableStateFlow(false)
    val usingHealthConnect: StateFlow<Boolean> = _usingHealthConnect

    /** ¿Hay Health Connect utilizable en este dispositivo? (para ofrecer activarlo en la UI) */
    val healthConnectAvailable: Boolean get() = healthConnect.isAvailable()

    /** Permisos de salud a solicitar desde la Activity. */
    val healthConnectPermissions: Set<String> get() = healthConnect.permissions

    /** Contract para lanzar la solicitud de permisos de salud desde la Activity. */
    fun healthConnectPermissionContract() = healthConnect.requestPermissionsContract()

    private fun publishJourney() {
        if (hcEnabled) return // en modo Health Connect el valor lo fija refreshFromHealthConnect()
        _journeySteps.value = (accumulated - journeyBaseline).coerceAtLeast(0L)
    }

    private val dailyHistoryLock = Any()
    private val dailyHistory: MutableMap<Long, Long> = HashMap()

    private val _dailySteps = MutableStateFlow<Map<Long, Long>>(emptyMap())
    override val dailySteps: StateFlow<Map<Long, Long>> = _dailySteps

    private val _todaySteps = MutableStateFlow(0L)
    override val todaySteps: StateFlow<Long> = _todaySteps

    private val hourlyLock = Any()
    private val hourly = LongArray(24)
    @Volatile private var hourlyEpochDay: Long = -1L

    private val _hourlySteps = MutableStateFlow<List<Long>>(List(24) { 0L })
    override val hourlySteps: StateFlow<List<Long>> = _hourlySteps

    private fun publishToday() {
        val todayEpoch = LocalDate.now().toEpochDay()
        _todaySteps.value = dailyHistory[todayEpoch] ?: 0L
    }

    /** Reinicia el registro por hora si el día almacenado no es hoy. Debe llamarse con hourlyLock. */
    private fun rollHourlyIfNewDay(todayEpoch: Long) {
        if (hourlyEpochDay != todayEpoch) {
            hourly.fill(0L)
            hourlyEpochDay = todayEpoch
        }
    }

    private fun publishHourly() {
        _hourlySteps.value = hourly.toList()
    }

    private val loadJob: Deferred<Unit> = scope.async {
        val prefs = appContext.dataStore.data.first()
        accumulated = prefs[Keys.ACCUMULATED] ?: 0L
        lastSensorValue = prefs[Keys.LAST_SENSOR] ?: -1L
        missionStartEpochDay = prefs[Keys.MISSION_START] ?: -1L
        _missionStart.value = if (missionStartEpochDay >= 0L) missionStartEpochDay else null

        // Viaje activo. Si no existe, es una instalación previa a las rutas múltiples:
        // se asume que todo el avance acumulado pertenece a la ida de Frodo (no se pierde nada).
        val storedRoute = prefs[Keys.ACTIVE_ROUTE]
        if (storedRoute == null) {
            activeRouteId = RouteId.FRODO
            activeDirectionValue = Direction.FORWARD
            journeyBaseline = 0L
            journeyStartEpochDay = missionStartEpochDay
            // Instalación nueva (sin pasos ni misión) → preguntar ruta en el primer arranque.
            // Usuario previo a las rutas múltiples (ya tenía progreso) → conserva Frodo, sin preguntar.
            routeChosenValue = accumulated > 0L || missionStartEpochDay >= 0L
        } else {
            activeRouteId = runCatching { RouteId.valueOf(storedRoute) }.getOrDefault(RouteId.FRODO)
            activeDirectionValue = runCatching {
                Direction.valueOf(prefs[Keys.ACTIVE_DIRECTION] ?: "FORWARD")
            }.getOrDefault(Direction.FORWARD)
            journeyBaseline = prefs[Keys.JOURNEY_BASELINE] ?: 0L
            journeyStartEpochDay = prefs[Keys.JOURNEY_START] ?: missionStartEpochDay
            routeChosenValue = prefs[Keys.ROUTE_CHOSEN] ?: true
        }
        _activeRoute.value = activeRouteId
        _activeDirection.value = activeDirectionValue
        _journeyStart.value = if (journeyStartEpochDay >= 0L) journeyStartEpochDay else null
        _routeChosen.value = routeChosenValue
        cineSeenValue = parseIntSet(prefs[Keys.CINE_SEEN])
        _cineSeen.value = cineSeenValue
        eventSeenValue = prefs[Keys.EVENT_SEEN].orEmpty().split(',').filter { it.isNotBlank() }
        _eventSeen.value = eventSeenValue
        eventCheckpointValue = (prefs[Keys.EVENT_CHECKPOINT] ?: 0L).toInt()
        _eventCheckpoint.value = eventCheckpointValue
        eventPendingValue = prefs[Keys.EVENT_PENDING]?.ifBlank { null }
        _eventPending.value = eventPendingValue
        hcEnabled = prefs[Keys.HC_ENABLED] ?: false
        hcAnchorMillis = prefs[Keys.HC_ANCHOR_MILLIS] ?: -1L
        hcAnchorOffset = prefs[Keys.HC_ANCHOR_OFFSET] ?: 0L
        _usingHealthConnect.value = hcEnabled
        publishJourney()
        // En modo HC mostramos al menos el progreso preservado hasta que llegue el primer
        // refresco real (evita un parpadeo a 0 al abrir la app).
        if (hcEnabled) _journeySteps.value = hcAnchorOffset.coerceAtLeast(0L)
        val historyRaw = prefs[Keys.DAILY_HISTORY].orEmpty()
        val migrated = prefs[Keys.MIGRATED_DAILY_V1] ?: false
        var migrationApplied = false
        synchronized(dailyHistoryLock) {
            dailyHistory.clear()
            dailyHistory.putAll(deserializeDailyHistory(historyRaw))
            if (!migrated && accumulated > 0L) {
                val attributed = dailyHistory.values.sum()
                val unattributed = accumulated - attributed
                if (unattributed > 0L) {
                    val yesterday = LocalDate.now().minusDays(1).toEpochDay()
                    dailyHistory[yesterday] = (dailyHistory[yesterday] ?: 0L) + unattributed
                }
                migrationApplied = true
            }
            _dailySteps.value = HashMap(dailyHistory)
            publishToday()
        }
        val hourlyRaw = prefs[Keys.HOURLY_TODAY].orEmpty()
        synchronized(hourlyLock) {
            deserializeHourly(hourlyRaw, hourly).let { storedDay -> hourlyEpochDay = storedDay }
            rollHourlyIfNewDay(LocalDate.now().toEpochDay())
            publishHourly()
        }
        loaded = true
        _totalSteps.value = accumulated
        if (migrationApplied || !migrated || storedRoute == null) persist(markMigrated = true)
    }

    override suspend fun ensureLoaded() {
        loadJob.await()
    }

    suspend fun startListening() {
        ensureLoaded()
        if (hcEnabled) {
            if (healthConnect.isAvailable() && healthConnect.hasPermission()) {
                _usingHealthConnect.value = true
                refreshFromHealthConnect()
                return // en modo HC no registramos el sensor: evita doble conteo
            }
            // Health Connect dejó de estar disponible o se revocó el permiso → caída al sensor.
            hcEnabled = false
            _usingHealthConnect.value = false
            publishJourney()
            persist()
        }
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Activa Health Connect como fuente de pasos (móvil + smartwatch). Debe llamarse tras
     * conceder el permiso de salud. Es idempotente: si ya estaba activo, solo refresca.
     * Al activarlo por primera vez fija el anchor en "ahora" y preserva el progreso actual
     * del viaje, de modo que el cambio de fuente no altera el avance en el mapa.
     *
     * @return true si quedó activo; false si Health Connect no está disponible o sin permiso.
     */
    suspend fun enableHealthConnect(): Boolean {
        ensureLoaded()
        if (!healthConnect.isAvailable() || !healthConnect.hasPermission()) return false
        if (!hcEnabled) {
            hcEnabled = true
            hcAnchorMillis = System.currentTimeMillis()
            hcAnchorOffset = (accumulated - journeyBaseline).coerceAtLeast(0L)
            _usingHealthConnect.value = true
            stopListening() // el sensor ya no hace falta
            persist()
        }
        refreshFromHealthConnect()
        return true
    }

    /** Relee los datos desde Health Connect (si el modo está activo). Seguro de llamar siempre. */
    suspend fun refresh() {
        ensureLoaded()
        if (hcEnabled) refreshFromHealthConnect()
    }

    private suspend fun refreshFromHealthConnect() {
        if (!hcEnabled) return
        val now = Instant.now()
        val anchor = if (hcAnchorMillis >= 0L) Instant.ofEpochMilli(hcAnchorMillis) else now
        val raw = healthConnect.aggregateSteps(anchor, now)
        _journeySteps.value = (raw + hcAnchorOffset).coerceAtLeast(0L)

        val todayEpoch = LocalDate.now().toEpochDay()
        if (missionStartEpochDay < 0L) {
            missionStartEpochDay = todayEpoch
            _missionStart.value = missionStartEpochDay
        }
        if (journeyStartEpochDay < 0L) {
            journeyStartEpochDay = todayEpoch
            _journeyStart.value = journeyStartEpochDay
        }

        // Histórico diario: Health Connect manda en la ventana reciente; conservamos los días
        // antiguos que ya tuviéramos del sensor (no se borran).
        val startDate = LocalDate.now().minusDays(HC_DAILY_WINDOW_DAYS)
        val buckets = healthConnect.dailyBuckets(startDate)
        if (buckets.isNotEmpty()) {
            synchronized(dailyHistoryLock) {
                for ((day, steps) in buckets) dailyHistory[day] = steps
                pruneOldDays(todayEpoch)
                _dailySteps.value = HashMap(dailyHistory)
                publishToday()
            }
        }

        // Reparto por hora del día de hoy.
        val hours = healthConnect.hourlyToday()
        synchronized(hourlyLock) {
            hourlyEpochDay = todayEpoch
            for (i in 0 until 24) hourly[i] = hours[i]
            publishHourly()
        }
        persist()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        if (!loaded) return
        val current = event.values[0].toLong()
        if (lastSensorValue < 0L) {
            lastSensorValue = current
            persist()
            return
        }
        val delta = if (current < lastSensorValue) current else current - lastSensorValue
        if (delta > 0L) {
            accumulated += delta
            _totalSteps.value = accumulated
            val todayEpochDay = LocalDate.now().toEpochDay()
            if (missionStartEpochDay < 0L) {
                missionStartEpochDay = todayEpochDay
                _missionStart.value = missionStartEpochDay
            }
            if (journeyStartEpochDay < 0L) {
                journeyStartEpochDay = todayEpochDay
                _journeyStart.value = journeyStartEpochDay
            }
            publishJourney()
            val todayEpoch = LocalDate.now().toEpochDay()
            synchronized(dailyHistoryLock) {
                val previous = dailyHistory[todayEpoch] ?: 0L
                dailyHistory[todayEpoch] = previous + delta
                pruneOldDays(todayEpoch)
                _dailySteps.value = HashMap(dailyHistory)
                publishToday()
            }
            synchronized(hourlyLock) {
                rollHourlyIfNewDay(todayEpoch)
                val hour = java.time.LocalTime.now().hour
                hourly[hour] += delta
                publishHourly()
            }
        }
        lastSensorValue = current
        persist()
    }

    private fun pruneOldDays(todayEpoch: Long) {
        val cutoff = todayEpoch - DAILY_HISTORY_MAX_DAYS
        val it = dailyHistory.entries.iterator()
        while (it.hasNext()) {
            if (it.next().key < cutoff) it.remove()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    /**
     * Inicia un nuevo viaje (cambio de ruta o vuelta a casa). El progreso de la ruta anterior se
     * descarta: el baseline se fija en los pasos acumulados actuales y el viaje arranca en hoy.
     */
    override suspend fun startJourney(route: RouteId, direction: Direction) {
        ensureLoaded()
        activeRouteId = route
        activeDirectionValue = direction
        journeyBaseline = accumulated
        journeyStartEpochDay = LocalDate.now().toEpochDay()
        if (hcEnabled) {
            // Nuevo viaje: contamos desde cero a partir de ahora.
            hcAnchorMillis = System.currentTimeMillis()
            hcAnchorOffset = 0L
        }
        routeChosenValue = true
        cineSeenValue = emptySet()
        eventSeenValue = emptyList()
        eventCheckpointValue = 0
        eventPendingValue = null
        _activeRoute.value = route
        _activeDirection.value = direction
        _journeyStart.value = journeyStartEpochDay
        _routeChosen.value = true
        _cineSeen.value = emptySet()
        _eventSeen.value = emptyList()
        _eventCheckpoint.value = 0
        _eventPending.value = null
        publishJourney()
        persist()
        if (hcEnabled) refreshFromHealthConnect()
    }

    /** Marca capítulos de cinemática como ya auto-reproducidos en el viaje activo. */
    override suspend fun markCineSeen(indices: Set<Int>) {
        ensureLoaded()
        if (indices.isEmpty()) return
        cineSeenValue = cineSeenValue + indices
        _cineSeen.value = cineSeenValue
        persist()
    }

    /** Guarda la resolución de sucesos: nuevo checkpoint, lista de vistos (en orden) y pendiente. */
    override suspend fun applyEventResolution(checkpoint: Int, seen: List<String>, pending: String?) {
        ensureLoaded()
        eventCheckpointValue = checkpoint
        eventSeenValue = seen
        eventPendingValue = pending
        _eventCheckpoint.value = checkpoint
        _eventSeen.value = seen
        _eventPending.value = pending
        persist()
    }

    /** Descarta el suceso pendiente (tras mostrarlo). */
    override suspend fun clearEventPending() {
        ensureLoaded()
        eventPendingValue = null
        _eventPending.value = null
        persist()
    }

    private fun persist(markMigrated: Boolean = false) {
        val snapshotAccumulated = accumulated
        val snapshotLast = lastSensorValue
        val snapshotStart = missionStartEpochDay
        val snapshotHistory = synchronized(dailyHistoryLock) { serializeDailyHistory(dailyHistory) }
        val snapshotHourly = synchronized(hourlyLock) { serializeHourly(hourlyEpochDay, hourly) }
        val snapshotRoute = activeRouteId
        val snapshotDirection = activeDirectionValue
        val snapshotBaseline = journeyBaseline
        val snapshotJourneyStart = journeyStartEpochDay
        val snapshotRouteChosen = routeChosenValue
        val snapshotCine = cineSeenValue
        val snapshotEventSeen = eventSeenValue
        val snapshotEventCp = eventCheckpointValue
        val snapshotEventPending = eventPendingValue
        val snapshotHcEnabled = hcEnabled
        val snapshotHcAnchorMillis = hcAnchorMillis
        val snapshotHcAnchorOffset = hcAnchorOffset
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[Keys.ACCUMULATED] = snapshotAccumulated
                prefs[Keys.LAST_SENSOR] = snapshotLast
                if (snapshotStart >= 0L) prefs[Keys.MISSION_START] = snapshotStart
                prefs[Keys.DAILY_HISTORY] = snapshotHistory
                prefs[Keys.HOURLY_TODAY] = snapshotHourly
                prefs[Keys.ACTIVE_ROUTE] = snapshotRoute.name
                prefs[Keys.ACTIVE_DIRECTION] = snapshotDirection.name
                prefs[Keys.JOURNEY_BASELINE] = snapshotBaseline
                if (snapshotJourneyStart >= 0L) prefs[Keys.JOURNEY_START] = snapshotJourneyStart
                prefs[Keys.ROUTE_CHOSEN] = snapshotRouteChosen
                prefs[Keys.CINE_SEEN] = snapshotCine.joinToString(",")
                prefs[Keys.EVENT_SEEN] = snapshotEventSeen.joinToString(",")
                prefs[Keys.EVENT_CHECKPOINT] = snapshotEventCp.toLong()
                prefs[Keys.EVENT_PENDING] = snapshotEventPending ?: ""
                prefs[Keys.HC_ENABLED] = snapshotHcEnabled
                if (snapshotHcAnchorMillis >= 0L) prefs[Keys.HC_ANCHOR_MILLIS] = snapshotHcAnchorMillis
                prefs[Keys.HC_ANCHOR_OFFSET] = snapshotHcAnchorOffset
                if (markMigrated) prefs[Keys.MIGRATED_DAILY_V1] = true
            }
        }
    }
}

private fun parseIntSet(raw: String?): Set<Int> =
    raw?.split(',')?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()

private fun serializeDailyHistory(map: Map<Long, Long>): String =
    map.entries.joinToString(";") { "${it.key}:${it.value}" }

private fun serializeHourly(epochDay: Long, hourly: LongArray): String =
    "$epochDay|" + hourly.joinToString(",")

/** Rellena [out] con los 24 valores almacenados y devuelve el epochDay guardado (-1 si vacío/ inválido). */
private fun deserializeHourly(raw: String, out: LongArray): Long {
    out.fill(0L)
    if (raw.isBlank()) return -1L
    val pipe = raw.indexOf('|')
    if (pipe <= 0) return -1L
    val day = raw.substring(0, pipe).toLongOrNull() ?: return -1L
    val parts = raw.substring(pipe + 1).split(',')
    for (i in 0 until minOf(24, parts.size)) {
        out[i] = parts[i].toLongOrNull() ?: 0L
    }
    return day
}

private fun deserializeDailyHistory(raw: String): Map<Long, Long> {
    if (raw.isBlank()) return emptyMap()
    val out = HashMap<Long, Long>()
    for (entry in raw.split(';')) {
        if (entry.isEmpty()) continue
        val colon = entry.indexOf(':')
        if (colon <= 0) continue
        val day = entry.substring(0, colon).toLongOrNull() ?: continue
        val count = entry.substring(colon + 1).toLongOrNull() ?: continue
        out[day] = count
    }
    return out
}
