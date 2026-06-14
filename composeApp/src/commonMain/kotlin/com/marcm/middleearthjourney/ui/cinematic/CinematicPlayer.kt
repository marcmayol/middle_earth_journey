package com.marcm.middleearthjourney.ui.cinematic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcm.middleearthjourney.ui.Cinzel
import com.marcm.middleearthjourney.ui.GoldBright
import com.marcm.middleearthjourney.ui.OnGold
import com.marcm.middleearthjourney.ui.TextPrimary
import com.marcm.middleearthjourney.ui.TextSecondary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlin.math.min


// ───────────────────────── Helpers de animación ─────────────────────────
private fun fadeAlpha(elapsed: Float, delay: Float, dur: Float): Float =
    ((elapsed - delay) / dur).coerceIn(0f, 1f)

private fun pulseAlpha(elapsed: Float, pulse: Pulse): Float {
    if (elapsed < pulse.phase) return 0f
    val tt = ((elapsed - pulse.phase) % pulse.period) / pulse.period
    val tri = if (tt < 0.5f) tt * 2f else (1f - tt) * 2f
    return 0.35f + 0.5f * tri
}

private data class RenderItem(val prim: Prim, val paths: List<Path>)

private fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

/** Parte un 'd' en subpaths por cada 'M' absoluta (la PathMeasure de Compose no tiene nextContour). */
private fun splitSubpaths(d: String): List<Path> {
    val parts = ArrayList<String>()
    val sb = StringBuilder()
    for (ch in d) {
        if (ch == 'M' && sb.isNotBlank()) {
            parts.add(sb.toString())
            sb.setLength(0)
        }
        sb.append(ch)
    }
    if (sb.isNotBlank()) parts.add(sb.toString())
    return parts.map { parse(it) }
}

private fun DrawScope.drawAnimatedStroke(paths: List<Path>, frac: Float, color: Color, width: Float) {
    val stroke = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (frac >= 1f) {
        paths.forEach { drawPath(it, color, style = stroke) }
        return
    }
    val pm = PathMeasure()
    val lengths = paths.map { pm.setPath(it, false); pm.length }
    val total = lengths.sum()
    if (total <= 0f) return
    val target = frac * total
    var acc = 0f
    for (i in paths.indices) {
        val len = lengths[i]
        when {
            acc + len <= target -> drawPath(paths[i], color, style = stroke)
            acc < target -> {
                pm.setPath(paths[i], false)
                val dest = Path()
                pm.getSegment(0f, target - acc, dest, true)
                drawPath(dest, color, style = stroke)
            }
            else -> {}
        }
        acc += len
        if (acc >= target) break
    }
}

@Composable
private fun SceneCanvas(prims: List<Prim>, elapsed: Float, modifier: Modifier = Modifier) {
    val items = remember(prims) {
        prims.map { p ->
            when (p) {
                is StrokePrim -> RenderItem(p, splitSubpaths(p.d))
                is FillPrim -> RenderItem(p, listOf(parse(p.d)))
                else -> RenderItem(p, emptyList())
            }
        }
    }
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val s = min(size.width / 376f, size.height / 860f)
        val ox = (size.width - 376f * s) / 2f
        val oy = (size.height - 860f * s) / 2f
        withTransform({ translate(ox, oy); scale(s, s, Offset.Zero) }) {
            items.forEach { item ->
                when (val p = item.prim) {
                    is StrokePrim -> {
                        val frac = fadeAlpha(elapsed, p.delay, p.dur)
                        if (frac > 0f) drawAnimatedStroke(item.paths, frac, p.color.copy(alpha = p.opacity), p.width)
                    }
                    is FillPrim -> {
                        val a = if (p.pulse != null) pulseAlpha(elapsed, p.pulse) else fadeAlpha(elapsed, p.delay, p.dur)
                        if (a > 0f && item.paths.isNotEmpty()) drawPath(item.paths[0], p.color.copy(alpha = (a * p.opacity).coerceIn(0f, 1f)))
                    }
                    else -> {}
                }
            }
        }
        // Texto (en espacio de pantalla, para que la fuente escale correctamente)
        prims.forEach { p ->
            if (p is TextPrim) {
                val a = fadeAlpha(elapsed, p.delay, p.dur)
                if (a > 0f) {
                    val style = TextStyle(color = p.color.copy(alpha = a), fontFamily = Cinzel, fontWeight = FontWeight.SemiBold, fontSize = (p.size * s).toSp())
                    val res = measurer.measure(p.text, style)
                    val cx = ox + p.x * s
                    val cy = oy + p.y * s
                    drawText(res, topLeft = Offset(cx - res.size.width / 2f, cy - res.size.height))
                }
            }
        }
    }
}

// ───────────────────────── Reproductor ─────────────────────────
@Composable
fun CinematicPlayer(
    collection: CineCollection,
    startChapter: Int,
    onExit: () -> Unit,
) {
    val narrator = rememberNarrator()
    DisposableEffect(Unit) { onDispose { narrator.shutdown() } }

    var chapter by remember { mutableIntStateOf(startChapter.coerceIn(0, collection.chapters.size - 1)) }
    var run by remember { mutableIntStateOf(0) }
    var sceneIndex by remember { mutableIntStateOf(0) }
    var ended by remember { mutableStateOf(false) }
    var jumpToEnd by remember { mutableStateOf(false) }
    var renderScene by remember { mutableStateOf(false) }
    var sceneClock by remember { mutableIntStateOf(0) }
    val elapsed = remember { mutableFloatStateOf(0f) }

    val chap = collection.chapters[chapter]

    // Reloj de la escena: arranca de 0 cada vez que cambia sceneClock (al sonar el audio).
    LaunchedEffect(sceneClock) {
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            elapsed.floatValue = ((now - start).coerceAtLeast(0L)) / 1_000_000_000f
        }
    }

    // Avance de escenas: espera a que el TTS esté listo y arranca cada escena AL sonar su audio.
    LaunchedEffect(chapter, run) {
        if (jumpToEnd) { jumpToEnd = false; ended = true; return@LaunchedEffect }
        ended = false
        renderScene = false
        // Espera a que el motor de voz esté inicializado (si no, se perdería el primer audio).
        var waited = 0
        while (!narrator.isReady && waited < 3000) { delay(60); waited += 60 }
        val scenes = chap.scenes
        for (i in scenes.indices) {
            sceneIndex = i
            val def = scenes[i]
            val minMs = (def.dur * 1000f).toLong()
            val started = CompletableDeferred<Unit>()
            val done = CompletableDeferred<Unit>()
            val spoke = narrator.speak(
                text = def.caption,
                onStart = { if (!started.isCompleted) started.complete(Unit) },
                done = { if (!done.isCompleted) done.complete(Unit) },
            )
            // Espera a que la voz EMPIECE para arrancar el dibujo en sincronía (con fallback).
            if (spoke) withTimeoutOrNull(2000L) { started.await() }
            renderScene = true
            sceneClock++
            val t0 = Clock.System.now().toEpochMilliseconds()
            if (spoke) withTimeoutOrNull(minMs + 12000L) { done.await() }
            val remain = (minMs - (Clock.System.now().toEpochMilliseconds() - t0)).coerceAtLeast(0L)
            delay(remain + 950L)
        }
        ended = true
    }

    val sceneDef = chap.scenes[sceneIndex.coerceIn(0, chap.scenes.size - 1)]
    val scenePrims = remember(chapter, sceneIndex, run) { buildScene(sceneDef.builder) }

    Box(Modifier.fillMaxSize().background(Color(0xFF0C0805))) {
        // Escenario pergamino
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFEFE3BB), Color(0xFFE3CF9E), Color(0xFFCDB074)),
                        center = Offset(0.46f * 1000f, 0.32f * 1000f),
                        radius = 1100f,
                    ),
                ),
        ) {
            if (renderScene) {
                SceneCanvas(prims = scenePrims, elapsed = elapsed.floatValue, modifier = Modifier.fillMaxSize())
            }
        }

        // Botón Saltar
        if (!ended) {
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x8C140E08))
                    .border(1.dp, Color(0x4DD6AF5C), RoundedCornerShape(22.dp))
                    .clickable { narrator.stop(); jumpToEnd = true; run++ }
                    .padding(horizontal = 15.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.Text("Saltar  »", style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(fontFamily = Cinzel), color = TextPrimary)
            }
        }

        // Scrim de subtítulos
        if (!ended && renderScene) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.42f to Color(0x9E120C07),
                            1f to Color(0xEB0C0805),
                        ),
                    )
                    .padding(top = 90.dp, start = 30.dp, end = 30.dp, bottom = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.material3.Text(
                    chap.title.uppercase(),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(fontFamily = Cinzel, letterSpacing = 3.sp),
                    color = Color(0xFFC1A45F),
                )
                Spacer(Modifier.height(14.dp))
                androidx.compose.material3.Text(
                    sceneDef.caption,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, lineHeight = 26.sp),
                    color = Color(0xFFEFE4C6),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                val frac = (sceneIndex + 1).toFloat() / chap.scenes.size
                val animFrac by animateFloatAsState(targetValue = frac, animationSpec = tween((sceneDef.dur * 1000f).toInt()), label = "prog")
                Box(
                    Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0x2ED6AF5C)),
                ) {
                    Box(Modifier.fillMaxWidth(animFrac).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFC99B3C), Color(0xFFE9CF82)))))
                }
            }
        }

        // Fin de capítulo
        AnimatedVisibility(visible = ended, enter = fadeIn(tween(500)), exit = fadeOut(tween(200))) {
            EndOverlay(
                chapterTitle = chap.title,
                onReplay = { narrator.stop(); run++ },
                onContinue = { narrator.stop(); onExit() },
            )
        }
    }
}

@Composable
private fun EndOverlay(
    chapterTitle: String,
    onReplay: () -> Unit,
    onContinue: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xE0181009), Color(0xF7080503)),
                    radius = 1200f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(40.dp)) {
            androidx.compose.material3.Text(
                "Capítulo completado".uppercase(),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(letterSpacing = 3.sp),
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.Text(chapterTitle, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = GoldBright)
            Spacer(Modifier.height(30.dp))
            // Continuar (CTA principal): cierra el reproductor y vuelve a la app.
            Box(
                Modifier.width(230.dp).clip(RoundedCornerShape(28.dp)).background(Brush.verticalGradient(listOf(Color(0xFFE6C572), Color(0xFFC99B3C)))).clickable(onClick = onContinue).padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text("Continuar", style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(fontFamily = Cinzel), color = OnGold)
            }
            Spacer(Modifier.height(11.dp))
            // Repetir (secundario).
            Box(
                Modifier.width(230.dp).clip(RoundedCornerShape(28.dp)).border(1.dp, Color(0x4DCDA349), RoundedCornerShape(28.dp)).clickable(onClick = onReplay).padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text("Repetir", style = androidx.compose.material3.MaterialTheme.typography.labelLarge.copy(fontFamily = Cinzel), color = Color(0xFFCBB486))
            }
        }
    }
}
