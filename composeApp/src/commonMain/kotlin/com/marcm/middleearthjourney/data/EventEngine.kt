package com.marcm.middleearthjourney.data

import kotlin.random.Random

/**
 * Lógica común de los sucesos aleatorios: cada 5 días de viaje, si ya se han andado 3 km
 * ese día, tira un dado (60%) y elige un suceso no visto (determinista por checkpoint).
 *
 * La usa iOS (en `IosJourneyRepository`); Android mantiene su propia copia en
 * `StepTrackingService` (background). Mismo algoritmo y semilla → mismos resultados.
 */
object EventEngine {
    const val PROBABILITY = 0.6f
    const val MIN_KM_FOR_EVENT = 3.0

    data class Result(val checkpoint: Int, val seen: List<String>, val pendingId: String?)

    /**
     * Devuelve la resolución de los checkpoints nuevos, o null si no hay nada que resolver
     * (no toca aún un checkpoint nuevo, o no se han andado 3 km hoy).
     */
    fun resolve(
        routeId: RouteId,
        direction: Direction,
        journeyStartEpochDay: Long,
        todayEpochDay: Long,
        todayKm: Double,
        lastCheckpoint: Int,
        seen: List<String>,
    ): Result? {
        if (journeyStartEpochDay < 0L) return null
        val daysElapsed = (todayEpochDay - journeyStartEpochDay).coerceAtLeast(0L).toInt() + 1
        val cp = daysElapsed / 5
        if (cp <= lastCheckpoint) return null
        if (todayKm < MIN_KM_FOR_EVENT) return null
        val pool = JourneyEvents.forRoute(routeId)
        val newSeen = seen.toMutableList()
        var pending: String? = null
        for (c in (lastCheckpoint + 1)..cp) {
            val seed = journeyStartEpochDay * 1_000_003L xor
                (routeId.ordinal.toLong() shl 16) xor (direction.ordinal.toLong() shl 8) xor c.toLong()
            val rng = Random(seed)
            if (rng.nextFloat() < PROBABILITY) {
                val unseen = pool.filter { it.id !in newSeen }
                if (unseen.isNotEmpty()) {
                    val pick = unseen[rng.nextInt(unseen.size)]
                    newSeen.add(pick.id)
                    if (c == cp) pending = pick.id
                }
            }
        }
        return Result(cp, newSeen, pending)
    }
}
