package com.marcm.middleearthjourney.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marcm.middleearthjourney.data.HcStatus
import com.marcm.middleearthjourney.data.StepRepository
import com.marcm.middleearthjourney.data.stepsToKm
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Sección "Health Connect" de Ajustes: dice de dónde salen los pasos y deja forzar la
 * conexión (útil si Health Connect se instaló después que la app), sincronizar en el acto
 * y recalcular el viaje entero con lo que tenga Health Connect.
 *
 * @param onConnect pide los permisos de salud y, si se conceden, activa el modo Health Connect.
 * @param onInstall abre la ficha de Health Connect en la tienda (no instalado o desactualizado).
 */
@Composable
fun HealthConnectSection(
    repo: StepRepository,
    onConnect: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val using by repo.usingHealthConnect.collectAsState()
    val journeySteps by repo.journeySteps.collectAsState()
    val journeyStart by repo.journeyStart.collectAsState()
    val heightCm by repo.heightCm.collectAsState()

    var status by remember { mutableStateOf(repo.healthConnectStatus()) }
    var hasHistory by remember { mutableStateOf(true) }
    // Se vuelve a mirar cada pocos segundos: el usuario puede instalar Health Connect o
    // conceder permisos fuera de la app y volver con Ajustes abierto.
    LaunchedEffect(Unit) {
        while (true) {
            status = repo.healthConnectStatus()
            hasHistory = repo.hasHealthConnectHistoryPermission()
            delay(2_000)
        }
    }

    var busy by remember { mutableStateOf(false) }
    var lastSync by remember { mutableStateOf<String?>(null) }
    var comparison by remember { mutableStateOf<Long?>(null) }

    Column(modifier.fillMaxWidth()) {
        Eyebrow("Health Connect")
        Spacer(Modifier.height(4.dp))
        Text(
            "Junta los pasos del móvil y del reloj. Sin conectar, la app cuenta solo con el sensor del móvil.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )

        Spacer(Modifier.height(12.dp))
        CodexCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            val (dot, label) = when {
                status == HcStatus.NO_INSTALADO -> TextFaint to "Health Connect no está instalado"
                status == HcStatus.ACTUALIZAR -> Ember to "Health Connect necesita actualizarse"
                using -> GoldBright to "Conectado: los pasos salen de Health Connect"
                else -> Ember to "Sin conectar: se usa el sensor del móvil"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(dot)
                Spacer(Modifier.width(10.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            }

            Spacer(Modifier.height(14.dp))
            when {
                status != HcStatus.DISPONIBLE -> CtaButton(
                    text = if (status == HcStatus.ACTUALIZAR) "Actualizar Health Connect" else "Instalar Health Connect",
                    onClick = onInstall,
                    modifier = Modifier.fillMaxWidth(),
                )

                !using -> CtaButton(
                    text = "Conectar con Health Connect",
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                )

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinePill(
                            text = "Sincronizar ahora",
                            onClick = {
                                scope.launch {
                                    busy = true
                                    repo.refresh()
                                    busy = false
                                    lastSync = "Hecho"
                                }
                            },
                        )
                        Spacer(Modifier.width(14.dp))
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = GoldBright,
                            )
                        } else {
                            lastSync?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = TextBody)
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinePill(
                        text = "Recalcular el viaje",
                        onClick = {
                            scope.launch {
                                busy = true
                                comparison = repo.healthConnectJourneySteps()
                                busy = false
                            }
                        },
                    )
                    if (!hasHistory) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Sin el permiso de historial, Health Connect solo deja leer los últimos 30 días.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinePill(text = "Dar permiso de historial", onClick = onConnect)
                    }
                }
            }
        }
    }

    comparison?.let { hcSteps ->
        val since = journeyStart?.let { formatDay(it) } ?: "el inicio del viaje"
        val nowKm = kmEs(stepsToKm(journeySteps, heightCm))
        val hcKm = kmEs(stepsToKm(hcSteps, heightCm))
        if (hcSteps <= journeySteps) {
            // Recalcular solo puede sumar: si Health Connect tiene menos (móvil nuevo, reloj
            // que aún no ha sincronizado...), usarlo borraría viaje. Se avisa y no se toca nada.
            CodexDialog(
                title = "Health Connect tiene menos",
                message = "Desde $since, Health Connect solo tiene ${intEs(hcSteps)} pasos ($hcKm km) " +
                    "y la app marca $nowKm km. El viaje se queda como está.\n\n" +
                    "Si el reloj aún no ha sincronizado, prueba más tarde.",
                confirmText = "Entendido",
                onConfirm = { comparison = null },
                onDismissRequest = { comparison = null },
            )
        } else {
            CodexDialog(
                title = "Recalcular el viaje",
                message = "Health Connect tiene ${intEs(hcSteps)} pasos ($hcKm km) desde $since. " +
                    "Ahora la app marca $nowKm km.\n\n" +
                    "Si lo usas, el viaje pasa a contar solo lo que hay en Health Connect.",
                confirmText = "Usar Health Connect",
                onConfirm = {
                    comparison = null
                    scope.launch { repo.rebuildFromHealthConnect() }
                },
                onDismissRequest = { comparison = null },
                dismissText = "Cancelar",
                onDismiss = { comparison = null },
                dismissOutlined = true,
            )
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Spacer(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

private val dayFormat = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))

private fun formatDay(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(dayFormat)
