package com.marcm.middleearthjourney.data

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlockKm: Double,
)

object Achievements {
    /** Logros conseguidos para un km dado dentro de una lista concreta de logros. */
    fun unlocked(list: List<Achievement>, distanceKm: Double): List<Achievement> =
        list.filter { it.unlockKm <= distanceKm }

    /** Siguiente logro por desbloquear dentro de una lista concreta de logros. */
    fun nextLocked(list: List<Achievement>, distanceKm: Double): Achievement? =
        list.firstOrNull { it.unlockKm > distanceKm }
}
