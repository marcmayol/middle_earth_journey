package com.marcm.middleearthjourney.ui.cinematic

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.graphics.PathMeasure as AndroidPathMeasure
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.min

// ───────────────────────── Narrador (Text-to-Speech) ─────────────────────────
class Narrator(context: Context) {
    private var ready = false
    private var onDone: (() -> Unit)? = null
    private var onStartCb: (() -> Unit)? = null
    private var finalId: String? = null
    private var firstId: String? = null
    private var counter = 0

    val isReady: Boolean get() = ready

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts.setLanguage(Locale("es", "ES"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale("es"))
                }
                tts.setSpeechRate(0.9f)  // más pausado
                tts.setPitch(0.8f)       // tono más grave → más masculino
                pickVoice()
                ready = true
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == firstId) { onStartCb?.invoke(); onStartCb = null }
            }
            override fun onDone(utteranceId: String?) {
                if (utteranceId == finalId) { onDone?.invoke(); onDone = null }
            }
            override fun onError(utteranceId: String?) {
                if (utteranceId == finalId) { onDone?.invoke(); onDone = null }
            }
        })
    }

    /**
     * Elige la mejor voz española: prioriza la que parezca masculina, de mayor calidad
     * y "enhanced/network" (suenan mucho menos robóticas que las básicas).
     */
    private fun pickVoice() {
        val voices = runCatching { tts.voices }.getOrNull() ?: return
        val es = voices.filter { v ->
            v.locale?.language == "es" &&
                v.features?.contains(android.speech.tts.TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
        }
        if (es.isEmpty()) return
        // Voces masculinas conocidas de Google (es-ES y es-US como respaldo), en orden de preferencia.
        val maleCodes = listOf("es-es-x-eed", "es-es-x-eee", "es-us-x-esd", "es-us-x-esc")
        val maleTokens = listOf("-m-", "_m_", "#male", "male", "-md-", "varon", "varón", "hombre")
        fun maleRank(v: android.speech.tts.Voice): Int {
            val byCode = maleCodes.indexOfFirst { v.name.startsWith(it, ignoreCase = true) }
            if (byCode >= 0) return byCode
            if (maleTokens.any { v.name.contains(it, ignoreCase = true) }) return maleCodes.size
            return Int.MAX_VALUE
        }
        val chosen = es.sortedWith(
            compareBy<android.speech.tts.Voice> { maleRank(it) }
                .thenByDescending { it.locale?.country.equals("ES", ignoreCase = true) }
                .thenBy { it.isNetworkConnectionRequired } // local: sin cortes de streaming, más fluida
                .thenByDescending { it.quality }
        ).firstOrNull()
        if (chosen != null) runCatching { tts.voice = chosen }
    }

    /** Parte el texto en oraciones para narrar con silencios entre ellas (menos robótico). */
    private fun splitSentences(t: String): List<String> {
        val parts = ArrayList<String>()
        val sb = StringBuilder()
        for (ch in t) {
            sb.append(ch)
            if (ch == '.' || ch == '!' || ch == '?' || ch == '…') {
                val s = sb.toString().trim()
                if (s.isNotEmpty()) parts.add(s)
                sb.setLength(0)
            }
        }
        val rest = sb.toString().trim()
        if (rest.isNotEmpty()) parts.add(rest)
        return parts
    }

    /** Devuelve true si la voz está disponible y empezó a hablar. [onStart] se llama al sonar la 1ª frase. */
    fun speak(text: String, onStart: () -> Unit = {}, done: () -> Unit): Boolean {
        if (!ready) return false
        onDone = done
        onStartCb = onStart
        counter++
        val base = "meej_$counter"
        val parts = splitSentences(text)
        if (parts.size <= 1) {
            firstId = base
            finalId = base
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, base)
            return true
        }
        firstId = "${base}_0"
        finalId = "${base}_${parts.size - 1}"
        parts.forEachIndexed { i, s ->
            tts.speak(s, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "${base}_$i")
            if (i < parts.size - 1) {
                tts.playSilentUtterance(420L, TextToSpeech.QUEUE_ADD, "${base}_sil_$i")
            }
        }
        return true
    }

    fun stop() {
        onDone = null
        runCatching { tts.stop() }
    }

    fun shutdown() {
        onDone = null
        runCatching { tts.stop(); tts.shutdown() }
    }
}

// ───────────────────────── Helpers de animación ─────────────────────────
private fun fadeAlpha(elapsed: Float, delay: Float, dur: Float): Float =
    ((elapsed - delay) / dur).coerceIn(0f, 1f)

private fun pulseAlpha(elapsed: Float, pulse: Pulse): Float {
    if (elapsed < pulse.phase) return 0f
    val tt = ((elapsed - pulse.phase) % pulse.period) / pulse.period
    val tri = if (tt < 0.5f) tt * 2f else (1f - tt) * 2f
    return 0.35f + 0.5f * tri
}

private data class RenderItem(val prim: Prim, val path: Path?)

private fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

private fun DrawScope.drawAnimatedStroke(path: Path, frac: Float, color: Color, width: Float) {
    if (frac >= 1f) {
        drawPath(path, color, style = Stroke(width = width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        return
    }
    val androidPath = path.asAndroidPath()
    val pm = AndroidPathMeasure(androidPath, false)
    var total = 0f
    do { total += pm.length } while (pm.nextContour())
    if (total <= 0f) return
    val target = frac * total
    val pm2 = AndroidPathMeasure(androidPath, false)
    val dest = android.graphics.Path()
    var acc = 0f
    do {
        val len = pm2.length
        when {
            acc + len <= target -> pm2.getSegment(0f, len, dest, true)
            acc < target -> pm2.getSegment(0f, target - acc, dest, true)
        }
        acc += len
    } while (pm2.nextContour() && acc < target)
    drawPath(dest.asComposePath(), color, style = Stroke(width = width, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
}

@Composable
private fun SceneCanvas(prims: List<Prim>, elapsed: Float, modifier: Modifier = Modifier) {
    val items = remember(prims) {
        prims.map { p ->
            when (p) {
                is StrokePrim -> RenderItem(p, parse(p.d))
                is FillPrim -> RenderItem(p, parse(p.d))
                else -> RenderItem(p, null)
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
                        if (frac > 0f) drawAnimatedStroke(item.path!!, frac, p.color.copy(alpha = p.opacity), p.width)
                    }
                    is FillPrim -> {
                        val a = if (p.pulse != null) pulseAlpha(elapsed, p.pulse) else fadeAlpha(elapsed, p.delay, p.dur)
                        if (a > 0f) drawPath(item.path!!, p.color.copy(alpha = (a * p.opacity).coerceIn(0f, 1f)))
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
    val context = LocalContext.current
    val narrator = remember { Narrator(context) }
    DisposableEffect(Unit) { onDispose { narrator.shutdown() } }

    var chapter by remember { mutableIntStateOf(startChapter.coerceIn(0, collection.chapters.size - 1)) }
    var run by remember { mutableIntStateOf(0) }
    var sceneIndex by remember { mutableIntStateOf(0) }
    var ended by remember { mutableStateOf(false) }
    var jumpToEnd by remember { mutableStateOf(false) }
    var renderScene by remember { mutableStateOf(false) }
    val sceneStartNanos = remember { mutableLongStateOf(0L) }
    val elapsed = remember { mutableFloatStateOf(0f) }

    val chap = collection.chapters[chapter]

    // Reloj continuo: el tiempo de la escena se mide desde sceneStartNanos (se reinicia al sonar el audio).
    LaunchedEffect(Unit) {
        while (true) {
            awaitFrame()
            elapsed.floatValue = ((System.nanoTime() - sceneStartNanos.longValue).coerceAtLeast(0L)) / 1_000_000_000f
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
            sceneStartNanos.longValue = System.nanoTime()
            renderScene = true
            val t0 = System.currentTimeMillis()
            if (spoke) withTimeoutOrNull(minMs + 12000L) { done.await() }
            val remain = (minMs - (System.currentTimeMillis() - t0)).coerceAtLeast(0L)
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
                    chap.title.uppercase(Locale("es", "ES")),
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
                "Capítulo completado".uppercase(Locale("es", "ES")),
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
