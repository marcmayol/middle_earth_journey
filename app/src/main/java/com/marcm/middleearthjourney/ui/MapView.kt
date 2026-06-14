package com.marcm.middleearthjourney.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcm.middleearthjourney.data.RouteId
import com.marcm.middleearthjourney.data.Waypoint
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

// ---------- Paleta del pergamino ----------
private val PaperLight = Color(0xFFEAD8AC)
private val PaperMid = Color(0xFFDDC68C)
private val PaperEdge = Color(0xFF7C5E32)
private val InkDark = Color(0xFF5A4222)
private val InkMid = Color(0xFF77592F)
private val InkSoft = Color(0xFF9A7C4A)
private val ForestGreen = Color(0xFF6E7C42)
private val MapRiverBlue = Color(0xFF6C8AA6)
private val RouteRed = Color(0xFFB1392B)
private val RouteBlue = Color(0xFF3E5BA6)
private val MordorShade = Color(0xFF6E4636)

@Composable
fun MiddleEarthMap(
    waypoints: List<Waypoint>,
    routeId: RouteId,
    currentIndex: Int,
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 360.dp,
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.7f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "pulse"
    )

    val measurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = size.width
        val h = size.height
        val corner = 18f

        val clip = Path().apply {
            addRoundRect(RoundRect(0f, 0f, w, h, corner, corner))
        }

        clipPath(clip) {
            // Base pergamino con degradado cálido
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(PaperLight, PaperMid),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                ),
                size = size,
            )
            drawParchmentTexture(w, h)

            // --- Mar y ríos (bajo el resto del relieve) ---
            drawWestSea(w, h)
            drawBrandywineRiver(w, h)
            drawAnduinRiver(w, h)
            drawEntwashRiver(w, h)

            // --- Bosques ---
            drawForestCluster(0.16f * w, 0.30f * h, spread = 26f, n = 14, seed = 11)   // La Comarca
            drawForestCluster(0.215f * w, 0.41f * h, spread = 14f, n = 8, seed = 12)    // Bosque Viejo
            drawMirkwood(w, h)                                                          // Bosque Negro
            drawForestCluster(0.50f * w, 0.57f * h, spread = 22f, n = 12, seed = 21)    // Fangorn
            drawForestCluster(0.55f * w, 0.53f * h, spread = 30f, n = 18, seed = 22)    // Lothlórien
            drawForestCluster(0.69f * w, 0.75f * h, spread = 24f, n = 14, seed = 23)    // Ithilien
            drawForestCluster(0.61f * w, 0.71f * h, spread = 16f, n = 9, seed = 24)     // Drúadan

            // --- Cordilleras ---
            drawBlueMountains(w, h)        // Ered Luin (oeste)
            drawNorthMountains(w, h)
            drawForodwaithMountains(w, h)  // norte helado
            drawMistyMountains(w, h)
            drawGreyMountains(w, h)        // noreste
            drawIronHills(w, h)            // este
            drawWhiteMountains(w, h)
            drawMordorMountains(w, h)
            drawHaradMountains(w, h)       // sur
            drawLonelyMountain(w, h)       // Erebor

            // --- Detalles de Mordor y criatura alada ---
            drawDeadMarshes(w, h)
            drawMordorShade(w, h)
            drawMountDoom(w, h)
            drawWingedCreature(w, h)

            // Etiquetas de región
            drawRegionLabels(measurer, w, h)

            // Ruta (azul para Bilbo, roja para Frodo, como en los mapas clásicos)
            val routeColor = if (routeId == RouteId.BILBO) RouteBlue else RouteRed
            drawRoute(waypoints, w, h, currentIndex, routeColor)

            // Marcadores
            waypoints.forEachIndexed { idx, wp ->
                drawWaypoint(wp, idx, currentIndex, w, h, pulse, routeColor)
            }

            // Adornos cartográficos
            drawCompassRose(0.88f * w, 0.15f * h, 16f, measurer)
            drawCartouche(measurer, w, h)

            // Marco decorativo (encima de todo)
            drawDecorativeFrame(w, h)

            // Viñeteado para envejecer los bordes
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Transparent, PaperEdge.copy(alpha = 0.55f)),
                    center = Offset(w / 2f, h / 2f),
                    radius = hypot(w, h) * 0.62f,
                ),
                size = size,
            )
        }
    }
}

// ---------- Textura y marco ----------

private fun DrawScope.drawParchmentTexture(w: Float, h: Float) {
    val r = Random(99)
    // manchas de envejecido
    repeat(60) {
        val x = r.nextFloat() * w
        val y = r.nextFloat() * h
        val rad = 3f + r.nextFloat() * 14f
        drawCircle(InkSoft.copy(alpha = 0.04f + r.nextFloat() * 0.05f), radius = rad, center = Offset(x, y))
    }
    // fibras finas
    repeat(40) {
        val x = r.nextFloat() * w
        val y = r.nextFloat() * h
        val len = 6f + r.nextFloat() * 18f
        drawLine(
            InkSoft.copy(alpha = 0.05f),
            Offset(x, y),
            Offset(x + len, y + (r.nextFloat() - 0.5f) * 4f),
            strokeWidth = 1f,
        )
    }
}

private fun DrawScope.drawDecorativeFrame(w: Float, h: Float) {
    val outer = 7f
    val inner = 13f
    drawRoundRect(
        color = InkDark,
        topLeft = Offset(outer, outer),
        size = Size(w - outer * 2, h - outer * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
        style = Stroke(width = 2.4f),
    )
    drawRoundRect(
        color = InkMid,
        topLeft = Offset(inner, inner),
        size = Size(w - inner * 2, h - inner * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
        style = Stroke(width = 1.2f),
    )
    // florituras en las esquinas
    val s = 18f
    val m = inner + 4f
    val corners = listOf(
        Offset(m, m) to Offset(1f, 1f),
        Offset(w - m, m) to Offset(-1f, 1f),
        Offset(m, h - m) to Offset(1f, -1f),
        Offset(w - m, h - m) to Offset(-1f, -1f),
    )
    for ((c, dir) in corners) {
        val p = Path().apply {
            moveTo(c.x + dir.x * s, c.y)
            quadraticBezierTo(c.x, c.y, c.x, c.y + dir.y * s)
        }
        drawPath(p, InkDark, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
        drawCircle(InkDark, radius = 2.2f, center = c)
    }
}

// ---------- Geografía ----------

private fun DrawScope.drawForestCluster(cx: Float, cy: Float, spread: Float, n: Int, seed: Int) {
    val r = Random(seed)
    repeat(n) {
        val ox = (r.nextFloat() - 0.5f) * spread * 2
        val oy = (r.nextFloat() - 0.5f) * spread
        val rad = 3.5f + r.nextFloat() * 2.5f
        val c = Offset(cx + ox, cy + oy)
        drawCircle(ForestGreen.copy(alpha = 0.55f), radius = rad, center = c)
        drawCircle(InkMid.copy(alpha = 0.5f), radius = rad, center = c, style = Stroke(width = 1f))
    }
}

private fun DrawScope.drawNorthMountains(w: Float, h: Float) {
    drawMountainRange(Offset(0.20f * w, 0.16f * h), Offset(0.40f * w, 0.16f * h), count = 7, baseHeight = 16f, jitter = 6f, seed = 41)
}

private fun DrawScope.drawMistyMountains(w: Float, h: Float) {
    // cadena vertical (norte-sur) por el centro
    drawMountainRange(Offset(0.40f * w, 0.20f * h), Offset(0.47f * w, 0.58f * h), count = 13, baseHeight = 24f, jitter = 9f, seed = 42)
}

private fun DrawScope.drawWhiteMountains(w: Float, h: Float) {
    drawMountainRange(Offset(0.47f * w, 0.63f * h), Offset(0.64f * w, 0.64f * h), count = 9, baseHeight = 20f, jitter = 7f, seed = 43)
}

private fun DrawScope.drawMordorMountains(w: Float, h: Float) {
    // muralla oeste (Ephel Dúath) vertical
    drawMountainRange(Offset(0.70f * w, 0.60f * h), Offset(0.74f * w, 0.80f * h), count = 8, baseHeight = 22f, jitter = 6f, seed = 44)
    // muralla norte (Ered Lithui) horizontal
    drawMountainRange(Offset(0.72f * w, 0.62f * h), Offset(0.90f * w, 0.63f * h), count = 9, baseHeight = 22f, jitter = 6f, seed = 45)
}

private fun DrawScope.drawAnduinRiver(w: Float, h: Float) {
    val path = Path().apply {
        moveTo(0.44f * w, 0.30f * h)
        cubicTo(0.50f * w, 0.40f * h, 0.52f * w, 0.46f * h, 0.56f * w, 0.54f * h)
        cubicTo(0.60f * w, 0.61f * h, 0.57f * w, 0.66f * h, 0.60f * w, 0.72f * h)
        cubicTo(0.62f * w, 0.77f * h, 0.58f * w, 0.82f * h, 0.55f * w, 0.90f * h)
    }
    drawPath(path, MapRiverBlue.copy(alpha = 0.85f), style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, MapRiverBlue.copy(alpha = 0.35f), style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawDeadMarshes(w: Float, h: Float) {
    val cx = 0.66f * w
    val cy = 0.69f * h
    val r = Random(7)
    for (i in 0 until 7) {
        val y = cy + (i - 3) * 4.5f
        drawLine(
            InkMid.copy(alpha = 0.55f),
            Offset(cx - 26f + r.nextFloat() * 4f, y),
            Offset(cx + 26f + r.nextFloat() * 4f, y),
            strokeWidth = 1.4f,
        )
    }
}

private fun DrawScope.drawMordorShade(w: Float, h: Float) {
    // sombreado interior de Mordor
    val r = Random(8)
    val cx = 0.81f * w
    val cy = 0.72f * h
    repeat(40) {
        val ox = (r.nextFloat() - 0.5f) * 120f
        val oy = (r.nextFloat() - 0.5f) * 60f
        drawCircle(MordorShade.copy(alpha = 0.10f), radius = 2f + r.nextFloat() * 3f, center = Offset(cx + ox, cy + oy))
    }
}

private fun DrawScope.drawMountDoom(w: Float, h: Float) {
    val cx = 0.85f * w
    val cy = 0.70f * h
    val path = Path().apply {
        moveTo(cx - 20f, cy + 14f)
        lineTo(cx, cy - 16f)
        lineTo(cx + 20f, cy + 14f)
        close()
    }
    drawPath(path, MordorShade.copy(alpha = 0.4f))
    drawPath(path, RouteRed.copy(alpha = 0.9f), style = Stroke(width = 2.4f, join = StrokeJoin.Round))
    // resplandor del cráter
    drawCircle(RouteRed.copy(alpha = 0.25f), radius = 10f, center = Offset(cx, cy - 12f))
    drawCircle(RouteRed, radius = 3.5f, center = Offset(cx, cy - 13f))
}

// ---------- Geografía propia de Bilbo ----------

private val DarkForest = Color(0xFF3F4A2A)

private fun DrawScope.drawMirkwood(w: Float, h: Float) {
    // Bosque Negro: masa densa y oscura
    val r = Random(31)
    val cx = 0.63f * w
    val cy = 0.25f * h
    repeat(26) {
        val ox = (r.nextFloat() - 0.5f) * 90f
        val oy = (r.nextFloat() - 0.5f) * 40f
        val rad = 4f + r.nextFloat() * 3f
        val c = Offset(cx + ox, cy + oy)
        drawCircle(DarkForest.copy(alpha = 0.7f), radius = rad, center = c)
        drawCircle(InkDark.copy(alpha = 0.6f), radius = rad, center = c, style = Stroke(width = 1f))
    }
}

private fun DrawScope.drawLonelyMountain(w: Float, h: Float) {
    // Erebor: un único pico aislado y prominente
    val cx = 0.88f * w
    val cy = 0.18f * h
    val hgt = 34f
    val halfW = hgt * 0.72f
    val apex = Offset(cx, cy - hgt)
    val left = Offset(cx - halfW, cy)
    val right = Offset(cx + halfW, cy)
    val tri = Path().apply {
        moveTo(left.x, left.y)
        lineTo(apex.x, apex.y)
        lineTo(right.x, right.y)
    }
    drawPath(tri, InkDark.copy(alpha = 0.5f))
    drawPath(tri, InkDark, style = Stroke(width = 2f, join = StrokeJoin.Round, cap = StrokeCap.Round))
    // cumbre nevada
    val snow = Path().apply {
        moveTo(apex.x, apex.y)
        lineTo(apex.x - 6f, apex.y + 9f)
        lineTo(apex.x + 6f, apex.y + 9f)
        close()
    }
    drawPath(snow, PaperLight)
}

// ---------- Mar, costa y ríos adicionales ----------

private fun DrawScope.drawWestSea(w: Float, h: Float) {
    // Olas tenues en el margen oeste (Belegaer)
    val r = Random(61)
    var y = 0.18f * h
    while (y < 0.78f * h) {
        val x0 = 0.012f * w
        val path = Path().apply {
            moveTo(x0, y)
            quadraticBezierTo(x0 + 6f, y - 3f, x0 + 12f, y)
            quadraticBezierTo(x0 + 18f, y + 3f, x0 + 24f, y)
        }
        drawPath(path, MapRiverBlue.copy(alpha = 0.4f), style = Stroke(width = 1.2f, cap = StrokeCap.Round))
        y += 10f + r.nextFloat() * 4f
    }
}

private fun DrawScope.drawBrandywineRiver(w: Float, h: Float) {
    val path = Path().apply {
        moveTo(0.205f * w, 0.24f * h)
        cubicTo(0.19f * w, 0.32f * h, 0.215f * w, 0.38f * h, 0.20f * w, 0.46f * h)
        cubicTo(0.19f * w, 0.52f * h, 0.215f * w, 0.56f * h, 0.205f * w, 0.62f * h)
    }
    drawPath(path, MapRiverBlue.copy(alpha = 0.8f), style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawEntwashRiver(w: Float, h: Float) {
    val path = Path().apply {
        moveTo(0.50f * w, 0.61f * h)
        quadraticBezierTo(0.54f * w, 0.64f * h, 0.575f * w, 0.665f * h)
    }
    drawPath(path, MapRiverBlue.copy(alpha = 0.7f), style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// ---------- Cordilleras adicionales ----------

private fun DrawScope.drawBlueMountains(w: Float, h: Float) {
    drawMountainRange(Offset(0.07f * w, 0.20f * h), Offset(0.085f * w, 0.46f * h), count = 9, baseHeight = 15f, jitter = 6f, seed = 51)
}

private fun DrawScope.drawForodwaithMountains(w: Float, h: Float) {
    drawMountainRange(Offset(0.30f * w, 0.085f * h), Offset(0.52f * w, 0.10f * h), count = 9, baseHeight = 13f, jitter = 6f, seed = 52)
}

private fun DrawScope.drawGreyMountains(w: Float, h: Float) {
    drawMountainRange(Offset(0.55f * w, 0.12f * h), Offset(0.80f * w, 0.14f * h), count = 11, baseHeight = 16f, jitter = 7f, seed = 53)
}

private fun DrawScope.drawIronHills(w: Float, h: Float) {
    drawMountainRange(Offset(0.90f * w, 0.27f * h), Offset(0.97f * w, 0.35f * h), count = 5, baseHeight = 13f, jitter = 5f, seed = 54)
}

private fun DrawScope.drawHaradMountains(w: Float, h: Float) {
    drawMountainRange(Offset(0.50f * w, 0.90f * h), Offset(0.74f * w, 0.90f * h), count = 8, baseHeight = 14f, jitter = 6f, seed = 55)
}

// ---------- Criatura alada (al noreste, como en los mapas clásicos) ----------

private fun DrawScope.drawWingedCreature(w: Float, h: Float) {
    val cx = 0.785f * w
    val cy = 0.095f * h
    val wing = Path().apply {
        moveTo(cx - 20f, cy)
        quadraticBezierTo(cx - 10f, cy - 9f, cx, cy)        // ala izquierda
        quadraticBezierTo(cx + 10f, cy - 9f, cx + 20f, cy)  // ala derecha
        quadraticBezierTo(cx + 10f, cy + 2f, cx, cy + 1.5f) // borde inferior derecho
        quadraticBezierTo(cx - 10f, cy + 2f, cx - 20f, cy)  // borde inferior izquierdo
        close()
    }
    drawPath(wing, InkDark.copy(alpha = 0.6f))
    // cola
    drawLine(InkDark.copy(alpha = 0.6f), Offset(cx, cy + 1.5f), Offset(cx, cy + 7f), strokeWidth = 1.4f, cap = StrokeCap.Round)
}

// ---------- Montañas con sombreado ----------

private fun DrawScope.drawMountainRange(
    from: Offset,
    to: Offset,
    count: Int,
    baseHeight: Float,
    jitter: Float,
    seed: Int,
) {
    val r = Random(seed)
    val dx = (to.x - from.x) / count
    val dy = (to.y - from.y) / count
    for (i in 0..count) {
        val cx = from.x + dx * i + (r.nextFloat() - 0.5f) * jitter
        val cy = from.y + dy * i + (r.nextFloat() - 0.5f) * jitter
        val hgt = baseHeight + (r.nextFloat() - 0.5f) * jitter
        val halfW = hgt * 0.78f
        val apex = Offset(cx, cy - hgt)
        val left = Offset(cx - halfW, cy)
        val right = Offset(cx + halfW, cy)

        // silueta
        val tri = Path().apply {
            moveTo(left.x, left.y)
            lineTo(apex.x, apex.y)
            lineTo(right.x, right.y)
        }
        drawPath(tri, InkDark.copy(alpha = 0.9f), style = Stroke(width = 1.7f, join = StrokeJoin.Round, cap = StrokeCap.Round))

        // sombreado de la ladera derecha (líneas paralelas)
        val shadeFrom = 0.30f
        var t = shadeFrom
        while (t < 0.95f) {
            val ax = apex.x + (right.x - apex.x) * t
            val ay = apex.y + (right.y - apex.y) * t
            val bx = ax - (hgt * 0.18f) * (1f - t)
            drawLine(InkMid.copy(alpha = 0.5f), Offset(ax, ay), Offset(bx, ay), strokeWidth = 1f)
            t += 0.22f
        }
        // pico nevado: pequeño trazo en la cima
        drawLine(PaperLight, Offset(apex.x - 2f, apex.y + 3f), Offset(apex.x + 2f, apex.y + 3f), strokeWidth = 1.4f)
    }
}

// ---------- Etiquetas ----------

private fun DrawScope.drawRegionLabels(measurer: TextMeasurer, w: Float, h: Float) {
    drawMapLabel(measurer, "FORODWAITH", 0.40f * w, 0.045f * h, sizeSp = 7f, spacing = 2f)
    drawMapLabel(measurer, "LA COMARCA", 0.15f * w, 0.245f * h, sizeSp = 9f, spacing = 1.5f)
    drawMapLabel(measurer, "ERIADOR", 0.31f * w, 0.45f * h, sizeSp = 9f, spacing = 2f)
    drawMapLabel(measurer, "MONTAÑAS NUBLADAS", 0.295f * w, 0.16f * h, sizeSp = 7f, spacing = 1f)
    drawMapLabel(measurer, "BOSQUE NEGRO", 0.63f * w, 0.315f * h, sizeSp = 8f, spacing = 1f, color = DarkForest)
    drawMapLabel(measurer, "EREBOR", 0.88f * w, 0.115f * h, sizeSp = 8f, spacing = 2f)
    drawMapLabel(measurer, "FANGORN", 0.49f * w, 0.605f * h, sizeSp = 7f, spacing = 1f, color = DarkForest)
    drawMapLabel(measurer, "ROHAN", 0.55f * w, 0.645f * h, sizeSp = 10f, spacing = 2f)
    drawMapLabel(measurer, "GONDOR", 0.62f * w, 0.78f * h, sizeSp = 10f, spacing = 2f)
    drawMapLabel(measurer, "RHÛN", 0.93f * w, 0.52f * h, sizeSp = 9f, spacing = 2f)
    drawMapLabel(measurer, "HARAD", 0.58f * w, 0.95f * h, sizeSp = 9f, spacing = 2f)
    drawMapLabel(measurer, "MORDOR", 0.80f * w, 0.55f * h, sizeSp = 14f, spacing = 4f, color = RouteRed.copy(alpha = 0.75f))
}

private fun DrawScope.drawMapLabel(
    measurer: TextMeasurer,
    text: String,
    cx: Float,
    cy: Float,
    sizeSp: Float,
    spacing: Float,
    color: Color = InkDark.copy(alpha = 0.72f),
) {
    val style = TextStyle(
        fontFamily = Cinzel,
        fontWeight = FontWeight.Medium,
        fontSize = sizeSp.sp,
        letterSpacing = spacing.sp,
        color = color,
    )
    val measured = measurer.measure(AnnotatedString(text), style)
    drawText(
        measurer,
        text,
        topLeft = Offset(cx - measured.size.width / 2f, cy - measured.size.height / 2f),
        style = style,
    )
}

// ---------- Rosa de los vientos y cartela ----------

private fun DrawScope.drawCompassRose(cx: Float, cy: Float, radius: Float, measurer: TextMeasurer) {
    drawCircle(InkDark.copy(alpha = 0.7f), radius = radius, center = Offset(cx, cy), style = Stroke(width = 1.4f))
    drawCircle(InkMid.copy(alpha = 0.5f), radius = radius * 0.62f, center = Offset(cx, cy), style = Stroke(width = 1f))
    // estrella de 4 puntas
    val long = radius * 1.05f
    val short = radius * 0.45f
    val star = Path().apply {
        moveTo(cx, cy - long)
        lineTo(cx + short, cy - short)
        lineTo(cx + long, cy)
        lineTo(cx + short, cy + short)
        lineTo(cx, cy + long)
        lineTo(cx - short, cy + short)
        lineTo(cx - long, cy)
        lineTo(cx - short, cy - short)
        close()
    }
    drawPath(star, InkDark.copy(alpha = 0.85f))
    drawPath(star, PaperLight, style = Stroke(width = 0.8f))
    // "N"
    val style = TextStyle(fontFamily = Cinzel, fontWeight = FontWeight.Bold, fontSize = 8.sp, color = InkDark)
    val m = measurer.measure(AnnotatedString("N"), style)
    drawText(measurer, "N", topLeft = Offset(cx - m.size.width / 2f, cy - long - m.size.height), style = style)
}

private fun DrawScope.drawCartouche(measurer: TextMeasurer, w: Float, h: Float) {
    val left = 0.05f * w
    val top = 0.83f * h
    val cw = 0.30f * w
    val ch = 0.13f * h
    drawRoundRect(
        color = PaperLight.copy(alpha = 0.75f),
        topLeft = Offset(left, top),
        size = Size(cw, ch),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
    )
    drawRoundRect(
        color = InkDark.copy(alpha = 0.8f),
        topLeft = Offset(left, top),
        size = Size(cw, ch),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
        style = Stroke(width = 1.6f),
    )
    drawRoundRect(
        color = InkMid.copy(alpha = 0.6f),
        topLeft = Offset(left + 3f, top + 3f),
        size = Size(cw - 6f, ch - 6f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
        style = Stroke(width = 0.8f),
    )
    val cx = left + cw / 2f
    drawMapLabel(measurer, "LA TIERRA", cx, top + ch * 0.36f, sizeSp = 11f, spacing = 2f, color = InkDark)
    drawMapLabel(measurer, "MEDIA", cx, top + ch * 0.68f, sizeSp = 11f, spacing = 2f, color = InkDark)
}

// ---------- Ruta y marcadores ----------

private fun DrawScope.drawRoute(
    waypoints: List<Waypoint>,
    w: Float,
    h: Float,
    currentIndex: Int,
    routeColor: Color,
) {
    val pts = waypoints.map { Offset(it.mapX * w, it.mapY * h) }

    // Tramo recorrido: sólido a mano alzada, con sombra suave debajo
    if (currentIndex > 0) {
        val travelled = sketchPath(pts.take(currentIndex + 1))
        drawPath(travelled, InkDark.copy(alpha = 0.25f), style = Stroke(width = 5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(travelled, routeColor, style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // Tramo pendiente: punteado tenue
    if (currentIndex < pts.size - 1) {
        val pending = sketchPath(pts.drop(currentIndex))
        drawPath(
            pending,
            routeColor.copy(alpha = 0.55f),
            style = Stroke(
                width = 2.4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                cap = StrokeCap.Round,
            )
        )
    }
}

private fun sketchPath(points: List<Offset>): Path {
    if (points.isEmpty()) return Path()
    val path = Path().apply { moveTo(points[0].x, points[0].y) }
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]
        val mid = Offset((prev.x + curr.x) / 2, (prev.y + curr.y) / 2)
        val perp = Offset(-(curr.y - prev.y), curr.x - prev.x)
        val len = hypot(perp.x, perp.y).coerceAtLeast(0.001f)
        val offset = 5f * sin(i.toFloat() * 1.7f)
        val ctrl = Offset(mid.x + perp.x / len * offset, mid.y + perp.y / len * offset)
        path.quadraticBezierTo(ctrl.x, ctrl.y, curr.x, curr.y)
    }
    return path
}

private fun DrawScope.drawWaypoint(
    wp: Waypoint,
    idx: Int,
    currentIndex: Int,
    w: Float,
    h: Float,
    pulse: Float,
    routeColor: Color,
) {
    val center = Offset(wp.mapX * w, wp.mapY * h)
    when {
        idx < currentIndex -> {
            drawCircle(routeColor, radius = 4f, center = center)
            drawCircle(PaperLight, radius = 4f, center = center, style = Stroke(width = 1.1f))
        }
        idx == currentIndex -> {
            drawCircle(routeColor.copy(alpha = 0.22f), radius = 18f * pulse, center = center)
            drawCircle(routeColor, radius = 7f, center = center)
            drawCircle(PaperLight, radius = 7f, center = center, style = Stroke(width = 2f))
            drawCircle(InkDark, radius = 9.5f, center = center, style = Stroke(width = 1.2f))
        }
        else -> {
            drawCircle(PaperLight, radius = 4f, center = center)
            drawCircle(InkDark.copy(alpha = 0.8f), radius = 4f, center = center, style = Stroke(width = 1.3f))
        }
    }
}
