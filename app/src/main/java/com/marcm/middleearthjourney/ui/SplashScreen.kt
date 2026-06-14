package com.marcm.middleearthjourney.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcm.middleearthjourney.R

/** Pantalla de bienvenida: marca con halo pulsante + wordmark + tagline. */
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
        modifier = modifier
            .fillMaxSize()
            .background(pageBackgroundBrush()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(220.dp)
                        .alpha(halo)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(GoldGlow.copy(alpha = 0.28f), Color.Transparent),
                            ),
                        ),
                )
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "Middle Earth Journey",
                    modifier = Modifier.size(160.dp),
                )
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
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = TextFaint,
            )
        }
    }
}
