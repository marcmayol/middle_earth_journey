@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.marcm.middleearthjourney.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.marcm.middleearthjourney.R

// ── Paleta heredada del mapa (MapView.kt sigue usándola) ──
val GoldRing = Color(0xFFD4A857)
val GoldRingBright = Color(0xFFEEC376)
val GoldRingDim = Color(0xFF8C6D2F)
val DarkParchment = Color(0xFF0E0B08)
val MidParchment = Color(0xFF1A1410)
val CardBg = Color(0xFF1F1813)
val Parchment = Color(0xFFE8DCC0)
val ParchmentMuted = Color(0xFFA89878)
val ElvenGreen = Color(0xFF7A8C5C)
val MordorRed = Color(0xFFA04030)
val MapInk = Color(0xFFB99A60)
val MapInkSoft = Color(0xFF8C7340)
val MapPaper = Color(0xFF120E0A)
val RiverBlue = Color(0xFF5C8FBF)

// ── Tipografía ──
// Cinzel: display, títulos, cifras, etiquetas de mapa (con moderación).
val Cinzel = FontFamily(
    Font(R.font.cinzel_regular, FontWeight.Normal),
    Font(R.font.cinzel_medium, FontWeight.Medium),
    Font(R.font.cinzel_bold, FontWeight.Bold),
)

// EB Garamond (variable): cuerpo, UI, datos, citas. Legible a tamaño pequeño.
private fun ebg(weight: Int) = FontVariation.Settings(FontVariation.weight(weight))
val EBGaramond = FontFamily(
    Font(R.font.eb_garamond_var, FontWeight.Normal, FontStyle.Normal, variationSettings = ebg(400)),
    Font(R.font.eb_garamond_var, FontWeight.Medium, FontStyle.Normal, variationSettings = ebg(500)),
    Font(R.font.eb_garamond_var, FontWeight.SemiBold, FontStyle.Normal, variationSettings = ebg(600)),
    Font(R.font.eb_garamond_italic_var, FontWeight.Normal, FontStyle.Italic, variationSettings = ebg(400)),
    Font(R.font.eb_garamond_italic_var, FontWeight.Medium, FontStyle.Italic, variationSettings = ebg(500)),
)

private val AppTypography = Typography(
    // Cinzel — display / títulos / cifras
    displayLarge = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 74.sp),
    displayMedium = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 48.sp),
    displaySmall = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 38.sp),
    headlineLarge = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = 0.5.sp),
    headlineMedium = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = 0.5.sp),
    headlineSmall = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 19.sp),
    titleLarge = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
    titleMedium = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    // EB Garamond — cuerpo / UI / datos
    bodyLarge = TextStyle(fontFamily = EBGaramond, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = EBGaramond, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = EBGaramond, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = EBGaramond, fontWeight = FontWeight.Medium, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = EBGaramond, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = EBGaramond, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

private val DarkScheme = darkColorScheme(
    primary = GoldBright,
    onPrimary = OnGold,
    primaryContainer = GoldDeep,
    onPrimaryContainer = TextPrimary,
    secondary = GoldBase,
    onSecondary = OnGold,
    background = BgBottom,
    onBackground = TextPrimary,
    surface = CardTop,
    onSurface = TextPrimary,
    surfaceVariant = CardQuietTop,
    onSurfaceVariant = TextSecondary,
    error = Negative,
    onError = TextPrimary,
    outline = TextFaint,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, typography = AppTypography, content = content)
}
