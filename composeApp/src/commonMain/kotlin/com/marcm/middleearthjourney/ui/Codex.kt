package com.marcm.middleearthjourney.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcm.middleearthjourney.data.RouteId

// ───────────────────────── Tokens de color "códice" ─────────────────────────
// Superficies y fondo
val BgTop = Color(0xFF231A10)
val BgMid = Color(0xFF160F09)
val BgBottom = Color(0xFF100B06)
val CardTop = Color(0xFF211910)
val CardBottom = Color(0xFF19120B)
val CardQuietTop = Color(0xFF1F1810)
val CardQuietBottom = Color(0xFF17110A)
val CellBg = Color(0xFF1A130C)
val NavTop = Color(0xFF16100A)
val NavBottom = Color(0xFF0E0A06)

// Dorado (acento de marca)
val GoldBase = Color(0xFFCDA349)
val GoldBright = Color(0xFFE6C572)
val GoldBrightest = Color(0xFFECCB78)
val GoldGlow = Color(0xFFFCF1C6)
val GoldDeep = Color(0xFFA87B2E)
val GoldDeep2 = Color(0xFFC99B3C)

// Bordes y divisores (dorado con alfa)
val GoldBorder = Color(0x24CDA349)      // ~.14
val GoldBorderSoft = Color(0x1FCDA349)  // ~.12
val GoldBorderStrong = Color(0x29CDA349) // ~.16
val GoldDivider = Color(0x1ACDA349)     // ~.10

// Texto
val TextPrimary = Color(0xFFECE3D0)
val TextSecondary = Color(0xFF9A8C72)
val TextFaint = Color(0xFF6E6453)
val TextBody = Color(0xFFC4B69C)

// Rutas y semántica
val RouteFrodo = Color(0xFFC7543F)
val RouteFrodoDeep = Color(0xFF7E271B)
val RouteBilbo = Color(0xFF5A87B8)
val RouteBilboDeep = Color(0xFF2F4F74)
val Ember = Color(0xFFED5A33)
val Negative = Color(0xFFCC6666)

// Tarjeta "Hoy" (bloque dorado macizo) y CTA
val HoyGold1 = Color(0xFFE9CF82)
val HoyGold2 = Color(0xFFD2A949)
val HoyGold3 = Color(0xFFC2933B)
val OnHoy = Color(0xFF2A1D0A)
val OnGold = Color(0xFF0F0A06)

// ───────────────────────── Brushes ─────────────────────────
val GoldCtaBrush = Brush.verticalGradient(listOf(GoldBright, GoldDeep2))
val HoyGoldBrush = Brush.linearGradient(listOf(HoyGold1, HoyGold2, HoyGold3))
val ProgressFillBrush = Brush.horizontalGradient(listOf(GoldDeep2, HoyGold1))
val CardBrush = Brush.verticalGradient(listOf(CardTop, CardBottom))
val CardQuietBrush = Brush.verticalGradient(listOf(CardQuietTop, CardQuietBottom))

/** Fondo de pantalla: degradado radial cálido con foco arriba-centro. */
fun pageBackgroundBrush(): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader = RadialGradientShader(
        center = Offset(size.width * 0.5f, 0f),
        radius = maxOf(size.width, size.height) * 1.15f,
        colors = listOf(BgTop, BgMid, BgBottom),
        colorStops = listOf(0f, 0.52f, 1f),
    )
}

fun routeColor(routeId: RouteId): Color =
    if (routeId == RouteId.FRODO) RouteFrodo else RouteBilbo

fun routeColorDeep(routeId: RouteId): Color =
    if (routeId == RouteId.FRODO) RouteFrodoDeep else RouteBilboDeep

// ───────────────────────── Formato numérico (español) ─────────────────────────
private val ES = java.util.Locale("es", "ES")

/** Entero con punto de millar: 13873 → "13.873". */
fun intEs(v: Long): String = String.format(ES, "%,d", v)

/** Decimal con coma y punto de millar: 2860.5 → "2.860,5". */
fun kmEs(v: Double, decimals: Int = 1): String = String.format(ES, "%,.${decimals}f", v)

// ───────────────────────── Componentes reutilizables ─────────────────────────

/** Etiqueta de sección (eyebrow): EB Garamond, versalitas, tracking amplio. */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextSecondary,
    align: TextAlign? = null,
) {
    Text(
        text = text.uppercase(java.util.Locale("es", "ES")),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.8.sp,
            fontWeight = FontWeight.Medium,
        ),
        color = color,
        textAlign = align,
    )
}

/** Tarjeta estándar del códice: degradado oscuro + borde dorado tenue. */
@Composable
fun CodexCard(
    modifier: Modifier = Modifier,
    brush: Brush = CardBrush,
    borderColor: Color = GoldBorder,
    radius: Dp = 20.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(brush)
            .border(1.dp, borderColor, shape)
            .padding(contentPadding),
        content = content,
    )
}

/** Botón primario (CTA dorado, pill, texto oscuro). */
@Composable
fun CtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(GoldCtaBrush)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = Cinzel,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
            color = OnGold,
        )
    }
}

/** Botón secundario contorneado (pill outline dorado). */
@Composable
fun OutlinePill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = GoldBright,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, color.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

/** Botón fantasma (texto dorado tenue, sin borde). */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
    }
}

/**
 * Diálogo del códice: tarjeta degradada cálida, borde dorado, esquinas 24dp.
 * [confirm] es el CTA dorado; [dismiss] es opcional (fantasma u outline).
 */
@Composable
fun CodexDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    dismissOutlined: Boolean = false,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2B2114), Color(0xFF211910))))
                .border(1.dp, Color(0x38CDA349), shape)
                .padding(24.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = TextBody)
            Spacer(Modifier.height(22.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dismissText != null && onDismiss != null) {
                    if (dismissOutlined) OutlinePill(dismissText, onDismiss) else GhostButton(dismissText, onDismiss)
                    Spacer(Modifier.width(10.dp))
                }
                CtaButton(confirmText, onConfirm)
            }
        }
    }
}

/** Barra de progreso fina con relleno dorado animado de 0 → valor. */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
    fill: Brush = ProgressFillBrush,
    track: Color = Color(0xFF241B11),
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        anim.animateTo(progress.coerceIn(0f, 1f), tween(600))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(track),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(anim.value)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(fill),
        )
    }
}
