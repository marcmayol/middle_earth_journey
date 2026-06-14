package com.marcm.middleearthjourney.ui.cinematic

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
actual fun rememberNarrator(): Narrator {
    val context = LocalContext.current
    return remember { AndroidNarrator(context) }
}

/** Narrador Android: TextToSpeech con voz masculina es-ES, pausas entre frases. */
class AndroidNarrator(context: Context) : Narrator {
    private var ready = false
    private var onDone: (() -> Unit)? = null
    private var onStartCb: (() -> Unit)? = null
    private var finalId: String? = null
    private var firstId: String? = null
    private var counter = 0

    override val isReady: Boolean get() = ready

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts.setLanguage(Locale("es", "ES"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale("es"))
                }
                tts.setSpeechRate(0.9f)
                tts.setPitch(0.8f)
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

    private fun pickVoice() {
        val voices = runCatching { tts.voices }.getOrNull() ?: return
        val es = voices.filter { v ->
            v.locale?.language == "es" &&
                v.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
        }
        if (es.isEmpty()) return
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
                .thenBy { it.isNetworkConnectionRequired }
                .thenByDescending { it.quality }
        ).firstOrNull()
        if (chosen != null) runCatching { tts.voice = chosen }
    }

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

    override fun speak(text: String, onStart: () -> Unit, done: () -> Unit): Boolean {
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

    override fun stop() {
        onDone = null
        runCatching { tts.stop() }
    }

    override fun shutdown() {
        onDone = null
        runCatching { tts.stop(); tts.shutdown() }
    }
}
