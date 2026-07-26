package com.marcm.middleearthjourney.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Pantalla de bienvenida: marca "Ruta Circular" (dibujada) con halo + wordmark + tagline. */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "splash")
    val halo by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "halo",
    )

    Box(
        modifier = modifier.fillMaxSize().background(pageBackgroundBrush()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(220.dp)
                        .alpha(halo)
                        .background(Brush.radialGradient(listOf(GoldGlow.copy(alpha = 0.28f), Color.Transparent))),
                )
                RutaCircularLogo(Modifier.size(160.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "MIDDLE EARTH",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp),
                color = TextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(Modifier.width(22.dp).height(1.dp).background(GoldBase.copy(alpha = 0.5f)))
                Text(
                    "JOURNEY",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 5.sp),
                    color = Color(0xFFD9B45C),
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                Box(Modifier.width(22.dp).height(1.dp).background(GoldBase.copy(alpha = 0.5f)))
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "tu viaje a cada paso",
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = TextFaint,
            )
        }
    }
}

/** Logo "Ruta Circular": anillo punteado + arco dorado + baliza (en viewBox 100x100). */
@Composable
private fun RutaCircularLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension / 100f
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        // Anillo punteado (lo que falta)
        drawCircle(
            color = Color(0xFF7D6C4A).copy(alpha = 0.6f),
            radius = 30f * s,
            center = p(50f, 50f),
            style = Stroke(width = 2.8f * s, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5f * s, 5f * s))),
        )
        // Baliza: halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF2C75E).copy(alpha = 0.55f), Color.Transparent),
                center = p(65f, 76f),
                radius = 13f * s,
            ),
            radius = 13f * s,
            center = p(65f, 76f),
        )
        // Arco dorado (lo andado): de las 12 (−90°) en sentido horario 150° hasta (65,76)
        drawArc(
            brush = Brush.linearGradient(listOf(Color(0xFFFCF1C6), Color(0xFFDCB154), Color(0xFF9A6E22))),
            startAngle = -90f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = p(20f, 20f),
            size = Size(60f * s, 60f * s),
            style = Stroke(width = 4.8f * s, cap = StrokeCap.Round),
        )
        // Punto de inicio (arriba)
        drawCircle(Color(0xFFFCF1C6), radius = 3.2f * s, center = p(50f, 20f))
        // Baliza: punto central
        drawCircle(Color(0xFFF2C75E), radius = 5.4f * s, center = p(65f, 76f))
        drawCircle(Color(0xFF130E08), radius = 5.4f * s, center = p(65f, 76f), style = Stroke(1.6f * s))
    }
}
