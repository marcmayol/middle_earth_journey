package com.marcm.middleearthjourney.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato común del repositorio del viaje. Expone los datos (todo en tipos
 * multiplataforma: pasos en Long, días como epochDay Long) y las acciones.
 *
 * Implementaciones:
 *  - Android: `StepRepository` (androidMain) con sensor `TYPE_STEP_COUNTER` + DataStore.
 *  - iOS (pendiente): con `CMPedometer` + `multiplatform-settings`/NSUserDefaults.
 *
 * Las fechas viajan como **epochDay** (Long); el ViewModel las convierte a
 * `kotlinx.datetime.LocalDate` con los helpers de `util/Dates.kt`.
 */
interface JourneyRepository {
    val journeySteps: StateFlow<Long>
    val journeyStart: StateFlow<Long?>
    val todaySteps: StateFlow<Long>
    val dailySteps: StateFlow<Map<Long, Long>>
    val hourlySteps: StateFlow<List<Long>>
    val activeRoute: StateFlow<RouteId>
    val activeDirection: StateFlow<Direction>
    val routeChosen: StateFlow<Boolean>
    val cineSeen: StateFlow<Set<Int>>
    val eventSeen: StateFlow<List<String>>
    val eventCheckpoint: StateFlow<Int>
    val eventPending: StateFlow<String?>

    // ---- Perfil corporal (Ajustes) ----
    /** Peso en kg; usado para estimar calorías. */
    val weightKg: StateFlow<Int>
    /** Altura en cm; determina la zancada (y por tanto los km del viaje) y afina las calorías. */
    val heightCm: StateFlow<Int>
    /** ¿Mostrar la quema de calorías en la app? (activado por defecto). */
    val showCalories: StateFlow<Boolean>

    /** ¿La plataforma expone un contador de pasos? */
    val hasSensor: Boolean

    suspend fun ensureLoaded()
    suspend fun startJourney(route: RouteId, direction: Direction)
    suspend fun markCineSeen(indices: Set<Int>)
    suspend fun applyEventResolution(checkpoint: Int, seen: List<String>, pending: String?)
    suspend fun clearEventPending()

    suspend fun setWeightKg(kg: Int)
    suspend fun setHeightCm(cm: Int)
    suspend fun setShowCalories(show: Boolean)
}
