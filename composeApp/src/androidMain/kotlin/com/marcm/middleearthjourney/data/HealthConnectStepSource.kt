package com.marcm.middleearthjourney.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.ZoneId

/**
 * Fuente de pasos basada en **Health Connect**, el almacén unificado de salud de Android.
 *
 * Es la fuente preferida frente al sensor `TYPE_STEP_COUNTER` del móvil porque agrega — ya
 * deduplicados — los pasos de TODAS las fuentes que escriben ahí: el propio teléfono y el
 * smartwatch (Wear OS / Fitbit / Google Fit). Así cuentan también los paseos hechos con el
 * reloj sin llevar el móvil encima.
 *
 * Toda esta API es exclusiva de Android; vive en androidMain. Si Health Connect no está
 * disponible o sin permiso, [StepRepository] cae automáticamente al sensor.
 */
class HealthConnectStepSource(private val appContext: Context) {

    /** Permiso imprescindible: lectura de pasos. */
    val readStepsPermission: String = HealthPermission.getReadPermission(StepsRecord::class)

    /**
     * Permiso de historial: sin él, Health Connect solo deja leer unos 30 días anteriores a la
     * concesión del permiso. Hace falta para recalcular el viaje entero (p. ej. en un móvil
     * nuevo). La librería alpha07 no trae la constante, así que va como texto.
     */
    val historyPermission: String = "android.permission.health.READ_HEALTH_DATA_HISTORY"

    /** Permisos que pedimos al conectar. Solo el de pasos es obligatorio. */
    val permissions: Set<String> = setOf(readStepsPermission, historyPermission)

    /** Estado de Health Connect en el dispositivo. */
    fun status(): HcStatus = when (HealthConnectClient.getSdkStatus(appContext)) {
        HealthConnectClient.SDK_AVAILABLE -> HcStatus.DISPONIBLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HcStatus.ACTUALIZAR
        else -> HcStatus.NO_INSTALADO
    }

    /** ¿Está Health Connect instalado y utilizable en este dispositivo? */
    fun isAvailable(): Boolean = status() == HcStatus.DISPONIBLE

    /**
     * Cliente; null si Health Connect no está disponible. Solo se guarda cuando se obtiene:
     * si se instala Health Connect con la app ya abierta, la siguiente llamada lo encuentra
     * (antes era un `lazy` que se quedaba con el null para toda la vida del proceso).
     */
    @Volatile private var cachedClient: HealthConnectClient? = null
    private val client: HealthConnectClient?
        get() {
            cachedClient?.let { return it }
            val c = runCatching {
                if (isAvailable()) HealthConnectClient.getOrCreate(appContext) else null
            }.getOrNull()
            cachedClient = c
            return c
        }

    /** Contract para lanzar la solicitud de permisos desde una Activity. */
    fun requestPermissionsContract() =
        PermissionController.createRequestPermissionResultContract()

    /** ¿Tenemos concedido el permiso de lectura de pasos? */
    suspend fun hasPermission(): Boolean = granted().contains(readStepsPermission)

    /** ¿Tenemos el permiso de historial completo? */
    suspend fun hasHistoryPermission(): Boolean = granted().contains(historyPermission)

    private suspend fun granted(): Set<String> {
        val c = client ?: return emptySet()
        return runCatching { c.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())
    }

    /** Total de pasos en el rango [start, end). Devuelve 0 ante cualquier fallo. */
    suspend fun aggregateSteps(start: Instant, end: Instant): Long {
        val c = client ?: return 0L
        if (!start.isBefore(end)) return 0L
        return runCatching {
            val response: AggregationResult = c.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        }.getOrDefault(0L)
    }

    /**
     * Pasos agregados por día (clave = epochDay local) entre [startDate] y hoy, ambos inclusive.
     * Solo incluye los días con datos.
     */
    suspend fun dailyBuckets(startDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Map<Long, Long> {
        val c = client ?: return emptyMap()
        val today = LocalDate.now(zoneId)
        if (startDate.isAfter(today)) return emptyMap()
        val start = startDate.atStartOfDay()
        val end = today.plusDays(1).atStartOfDay() // exclusivo: hasta el final de hoy
        return runCatching {
            val groups = c.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Period.ofDays(1),
                )
            )
            val out = HashMap<Long, Long>()
            for (g in groups) {
                val steps = g.result[StepsRecord.COUNT_TOTAL] ?: continue
                if (steps <= 0L) continue
                out[g.startTime.toLocalDate().toEpochDay()] = steps
            }
            out
        }.getOrDefault(emptyMap())
    }

    /** Pasos por hora del día de hoy (índice 0..23, hora local). */
    suspend fun hourlyToday(zoneId: ZoneId = ZoneId.systemDefault()): LongArray {
        val c = client ?: return LongArray(24)
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant()
        val nowInstant = Instant.now()
        val endOfDay = today.atTime(LocalTime.MAX).atZone(zoneId).toInstant()
        val end = if (nowInstant.isBefore(endOfDay)) nowInstant else endOfDay
        val out = LongArray(24)
        if (!start.isBefore(end)) return out
        runCatching {
            val groups = c.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Duration.ofHours(1),
                )
            )
            for (g in groups) {
                val steps = g.result[StepsRecord.COUNT_TOTAL] ?: continue
                val hour = LocalDateTime.ofInstant(g.startTime, zoneId).hour
                if (hour in 0..23) out[hour] += steps
            }
        }
        return out
    }
}

/** Estado de Health Connect en el dispositivo. */
enum class HcStatus { DISPONIBLE, ACTUALIZAR, NO_INSTALADO }
