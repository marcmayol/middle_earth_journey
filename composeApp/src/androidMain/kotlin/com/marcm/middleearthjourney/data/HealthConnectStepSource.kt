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

    /** Permisos que pedimos: solo lectura de pasos. */
    val permissions: Set<String> = setOf(HealthPermission.getReadPermission(StepsRecord::class))

    /** ¿Está Health Connect instalado y utilizable en este dispositivo? */
    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(appContext) == HealthConnectClient.SDK_AVAILABLE

    /** Cliente perezoso; null si Health Connect no está disponible. */
    private val client: HealthConnectClient? by lazy {
        runCatching { if (isAvailable()) HealthConnectClient.getOrCreate(appContext) else null }
            .getOrNull()
    }

    /** Contract para lanzar la solicitud de permisos desde una Activity. */
    fun requestPermissionsContract() =
        PermissionController.createRequestPermissionResultContract()

    /** ¿Tenemos concedido el permiso de lectura de pasos? */
    suspend fun hasPermission(): Boolean {
        val c = client ?: return false
        return runCatching {
            c.permissionController.getGrantedPermissions().containsAll(permissions)
        }.getOrDefault(false)
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
