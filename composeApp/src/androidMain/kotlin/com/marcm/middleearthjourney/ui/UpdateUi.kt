package com.marcm.middleearthjourney.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marcm.actualizador.Actualizador
import com.marcm.actualizador.EstadoActualizacion
import com.marcm.actualizador.Modo
import com.marcm.actualizador.TipoError
import com.marcm.middleearthjourney.BuildConfig
import kotlinx.coroutines.launch

/**
 * Aviso de versión nueva bajo la cabecera: una tarjeta del códice, sin diálogos ni nada
 * que corte el paso. Solo aparece cuando hay algo que anunciar o algo en marcha; los
 * errores y el "estás al día" viven en Ajustes, donde el usuario ha pedido comprobar.
 */
@Composable
fun UpdateBanner(
    estado: EstadoActualizacion,
    onActualizar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val marco = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)

    when (estado) {
        is EstadoActualizacion.Disponible -> CodexCard(
            modifier = marco,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoStories,
                    contentDescription = null,
                    tint = GoldBright,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow("Nuevo capítulo")
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Versión ${estado.info.versionName} disponible",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    if (estado.info.notas.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            estado.info.notas,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextBody,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                CtaButton(text = "Actualizar", onClick = onActualizar)
            }
        }

        is EstadoActualizacion.Descargando -> CodexCard(
            modifier = marco,
            brush = CardQuietBrush,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Avance("Descargando la nueva versión… ${estado.porcentaje} %", estado.porcentaje / 100f)
        }

        EstadoActualizacion.Verificando -> CodexCard(
            modifier = marco,
            brush = CardQuietBrush,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Avance("Comprobando que la copia es íntegra…", null)
        }

        EstadoActualizacion.Instalando -> CodexCard(
            modifier = marco,
            brush = CardQuietBrush,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Avance("Instalando…", null)
        }

        // Inactivo, Comprobando, AlDia, PidiendoPermiso y Error no pintan nada.
        else -> Unit
    }
}

@Composable
private fun Avance(texto: String, progreso: Float?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = TextBody)
        if (progreso != null) {
            LinearProgressIndicator(
                progress = { progreso },
                color = GoldBright,
                trackColor = CellBg,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearProgressIndicator(
                color = GoldBright,
                trackColor = CellBg,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Sección "Actualizaciones" de Ajustes: el ajuste de búsqueda automática, la comprobación
 * manual y la versión instalada. Es el **único** sitio de la app que informa de errores
 * o de que ya estás al día; las comprobaciones automáticas callan siempre.
 */
@Composable
fun UpdateSettingsSection(actualizador: Actualizador, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val estado by actualizador.estado.collectAsState()
    var buscarAuto by remember { mutableStateOf(actualizador.buscarAutomaticamente) }

    Column(modifier.fillMaxWidth()) {
        Eyebrow("Actualizaciones")
        Spacer(Modifier.height(4.dp))
        Text(
            "La app no está en Play Store: se actualiza sola comprobando si hay una versión nueva.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )

        Spacer(Modifier.height(12.dp))
        CodexCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Buscar actualizaciones",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = buscarAuto,
                    onCheckedChange = {
                        buscarAuto = it
                        actualizador.buscarAutomaticamente = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnGold,
                        checkedTrackColor = GoldBase,
                        checkedBorderColor = GoldBorderStrong,
                        uncheckedThumbColor = TextFaint,
                        uncheckedTrackColor = CellBg,
                        uncheckedBorderColor = GoldBorderSoft,
                    ),
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinePill(
                    text = "Buscar ahora",
                    onClick = { scope.launch { actualizador.comprobar(Modo.MANUAL) } },
                )
                Spacer(Modifier.width(14.dp))
                ResultadoComprobacion(estado)
            }

            if (estado is EstadoActualizacion.Disponible) {
                Spacer(Modifier.height(14.dp))
                CtaButton(
                    text = "Descargar e instalar",
                    onClick = { actualizador.actualizarAhora() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Versión instalada: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
            color = TextFaint,
        )
    }
}

/** Resultado de la comprobación manual, en una línea junto al botón. */
@Composable
private fun ResultadoComprobacion(estado: EstadoActualizacion) {
    if (estado == EstadoActualizacion.Comprobando) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = GoldBright,
        )
        return
    }
    val texto = when (estado) {
        EstadoActualizacion.AlDia -> "Estás al día"
        is EstadoActualizacion.Disponible -> "Versión ${estado.info.versionName} disponible"
        is EstadoActualizacion.Descargando -> "Descargando… ${estado.porcentaje} %"
        EstadoActualizacion.Verificando -> "Comprobando la copia…"
        EstadoActualizacion.Instalando -> "Instalando…"
        EstadoActualizacion.PidiendoPermiso -> "Concede el permiso para instalar"
        is EstadoActualizacion.Error -> when (estado.tipo) {
            TipoError.SIN_RED -> "Sin conexión"
            TipoError.HTTP -> "El servidor no responde"
            TipoError.MANIFIESTO -> "El aviso de versiones es ilegible"
            TipoError.DESCARGA -> "No se pudo descargar"
            TipoError.HASH -> "La descarga no era íntegra; se ha borrado"
            TipoError.INSTALACION -> "No se pudo instalar"
        }
        else -> ""
    }
    if (texto.isNotBlank()) {
        Text(texto, style = MaterialTheme.typography.bodySmall, color = TextBody)
    }
}
