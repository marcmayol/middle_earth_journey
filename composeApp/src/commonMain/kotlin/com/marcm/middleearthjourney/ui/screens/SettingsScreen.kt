package com.marcm.middleearthjourney.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcm.middleearthjourney.data.HEIGHT_RANGE
import com.marcm.middleearthjourney.data.WEIGHT_RANGE
import com.marcm.middleearthjourney.ui.CellBg
import com.marcm.middleearthjourney.ui.CodexCard
import com.marcm.middleearthjourney.ui.CardQuietBrush
import com.marcm.middleearthjourney.ui.Eyebrow
import com.marcm.middleearthjourney.ui.GoldBorder
import com.marcm.middleearthjourney.ui.GoldBright
import com.marcm.middleearthjourney.ui.GoldDivider
import com.marcm.middleearthjourney.ui.OnGold
import com.marcm.middleearthjourney.ui.TextBody
import com.marcm.middleearthjourney.ui.TextFaint
import com.marcm.middleearthjourney.ui.TextPrimary
import com.marcm.middleearthjourney.ui.TextSecondary
import com.marcm.middleearthjourney.ui.pageBackgroundBrush

/**
 * Pantalla de ajustes (overlay a pantalla completa). Permite editar el perfil corporal
 * (peso y altura) y activar/desactivar la quema de calorías. La altura define la zancada,
 * así que también afina los km del viaje.
 *
 * @param updateSection sección de actualizaciones, que aporta la plataforma (solo Android;
 *        ver la documentación de `App`). Aquí es el único sitio donde una comprobación
 *        informa de errores o de que ya estás al día.
 */
@Composable
fun SettingsScreen(
    weightKg: Int,
    heightCm: Int,
    showCalories: Boolean,
    onWeightChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onShowCaloriesChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    updateSection: @Composable () -> Unit = {},
) {
    Box(Modifier.fillMaxSize().background(pageBackgroundBrush())) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, GoldBorder, CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = GoldBright,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    "Ajustes",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                    color = TextPrimary,
                )
            }

            Spacer(Modifier.height(22.dp))
            Eyebrow("Tu perfil")
            Spacer(Modifier.height(4.dp))
            Text(
                "Tus pasos miden la zancada según tu altura, y con tu peso estimamos las calorías que quemas en el camino.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Spacer(Modifier.height(16.dp))

            AdjustRow(
                label = "Peso",
                value = "$weightKg kg",
                onMinus = { onWeightChange(weightKg - 1) },
                onPlus = { onWeightChange(weightKg + 1) },
                minusEnabled = weightKg > WEIGHT_RANGE.first,
                plusEnabled = weightKg < WEIGHT_RANGE.last,
            )
            Spacer(Modifier.height(12.dp))
            AdjustRow(
                label = "Altura",
                value = "$heightCm cm",
                onMinus = { onHeightChange(heightCm - 1) },
                onPlus = { onHeightChange(heightCm + 1) },
                minusEnabled = heightCm > HEIGHT_RANGE.first,
                plusEnabled = heightCm < HEIGHT_RANGE.last,
            )

            Spacer(Modifier.height(26.dp))
            Eyebrow("Calorías")
            Spacer(Modifier.height(10.dp))
            CodexCard(
                modifier = Modifier.fillMaxWidth(),
                brush = CardQuietBrush,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Mostrar quema de calorías",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Su cartel en Hoy y su apartado en Estadísticas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextFaint,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = showCalories,
                        onCheckedChange = onShowCaloriesChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnGold,
                            checkedTrackColor = GoldBright,
                            checkedBorderColor = GoldBright,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CellBg,
                            uncheckedBorderColor = GoldDivider,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "La quema de calorías es una estimación orientativa: depende del ritmo, el terreno y tu cuerpo.",
                style = MaterialTheme.typography.bodySmall,
                color = TextFaint,
                textAlign = TextAlign.Start,
            )

            Spacer(Modifier.height(28.dp))
            updateSection()

            Spacer(Modifier.height(28.dp))
            CodexCard(
                modifier = Modifier.fillMaxWidth(),
                brush = CardQuietBrush,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = GoldBright,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Eyebrow("Privacidad")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tus pasos, tu viaje y tu perfil no salen de este dispositivo. " +
                                "Lo único que la app consulta fuera es si existe una versión nueva.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextBody,
                        )
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun AdjustRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusEnabled: Boolean,
    plusEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CellBg)
            .border(1.dp, GoldBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Eyebrow(label)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = GoldBright)
        }
        StepButton(Icons.Filled.Remove, "Menos", onMinus, minusEnabled)
        Spacer(Modifier.width(12.dp))
        StepButton(Icons.Filled.Add, "Más", onPlus, plusEnabled)
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val tint = if (enabled) GoldBright else TextFaint
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .border(1.dp, if (enabled) GoldBorder else Color.Transparent, CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}
