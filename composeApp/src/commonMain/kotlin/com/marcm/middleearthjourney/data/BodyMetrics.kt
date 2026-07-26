package com.marcm.middleearthjourney.data

/**
 * Perfil corporal del viajero y conversiones derivadas (zancada, distancia, calorías).
 *
 * La altura determina la longitud de zancada real, de modo que los km del viaje se calculan
 * a partir de ella (un viajero más alto avanza más por paso). El peso se usa solo para estimar
 * las calorías quemadas. Ambos valores son configurables desde Ajustes.
 */

/** Valores por defecto del perfil (hasta que el usuario los ajuste). 170 cm ≈ 0,70 m/paso. */
const val DEFAULT_WEIGHT_KG: Int = 70
const val DEFAULT_HEIGHT_CM: Int = 170

/** Rangos admitidos en los ajustes (límites sensatos para evitar valores absurdos). */
val WEIGHT_RANGE: IntRange = 30..250
val HEIGHT_RANGE: IntRange = 120..220

/**
 * Factor antropométrico clásico: la longitud de zancada al caminar es ≈ altura × 0,414
 * (≈0,413 en mujeres y ≈0,415 en hombres; 0,414 es la media unisex).
 */
private const val STRIDE_FACTOR = 0.414

/**
 * Energía estimada al caminar ≈ 0,75 kcal por kg de peso y por km recorrido. Es una estimación
 * de gasto activo: el valor real varía con el ritmo, la pendiente y la persona, así que se
 * presenta siempre como aproximación.
 */
private const val KCAL_PER_KG_PER_KM = 0.75

/** Longitud de zancada (m) estimada a partir de la altura. 170 cm → ≈0,70 m. */
fun strideMeters(heightCm: Int): Double = heightCm * STRIDE_FACTOR / 100.0

/** Convierte pasos a km usando la zancada real estimada desde la altura. */
fun stepsToKm(steps: Long, heightCm: Int = DEFAULT_HEIGHT_CM): Double =
    steps * strideMeters(heightCm) / 1000.0

/** Calorías estimadas quemadas para unos pasos, según peso y altura del perfil. */
fun caloriesBurned(steps: Long, weightKg: Int, heightCm: Int): Double =
    stepsToKm(steps, heightCm) * weightKg * KCAL_PER_KG_PER_KM
