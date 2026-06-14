package com.marcm.middleearthjourney.ui.cinematic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVSpeechBoundaryImmediate
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesisVoiceGenderMale
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.darwin.NSObject

@Composable
actual fun rememberNarrator(): Narrator = remember { IosNarrator() }

/**
 * Narrador iOS con AVSpeechSynthesizer (voz es-ES, preferiblemente masculina).
 * NOTA (para probar en el dispositivo): partir el texto en frases con pausas, igual que el
 * Android `AndroidNarrator`, mejora el resultado; aquí se narra de una pieza por simplicidad.
 */
@OptIn(ExperimentalForeignApi::class)
class IosNarrator : Narrator {

    private val synth = AVSpeechSynthesizer()
    private var onDoneCb: (() -> Unit)? = null
    private var onStartCb: (() -> Unit)? = null

    private val voice: AVSpeechSynthesisVoice? by lazy {
        val all = AVSpeechSynthesisVoice.speechVoices().filterIsInstance<AVSpeechSynthesisVoice>()
        val es = all.filter { it.language.startsWith("es") }
        // Preferir es-ES masculina; si no, cualquier es-ES; si no, cualquier es.
        es.firstOrNull { it.language == "es-ES" && it.gender == AVSpeechSynthesisVoiceGenderMale }
            ?: es.firstOrNull { it.language == "es-ES" }
            ?: es.firstOrNull()
            ?: AVSpeechSynthesisVoice.voiceWithLanguage("es-ES")
    }

    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didStartSpeechUtterance: AVSpeechUtterance) {
            onStartCb?.invoke(); onStartCb = null
        }
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didFinishSpeechUtterance: AVSpeechUtterance) {
            onDoneCb?.invoke(); onDoneCb = null
        }
        override fun speechSynthesizer(synthesizer: AVSpeechSynthesizer, didCancelSpeechUtterance: AVSpeechUtterance) {
            onDoneCb?.invoke(); onDoneCb = null
        }
    }

    init {
        synth.delegate = delegate
    }

    override val isReady: Boolean get() = true

    override fun speak(text: String, onStart: () -> Unit, done: () -> Unit): Boolean {
        onStartCb = onStart
        onDoneCb = done
        val u = AVSpeechUtterance.speechUtteranceWithString(text)
        voice?.let { u.voice = it }
        u.rate = 0.45f          // AVSpeechUtteranceDefaultSpeechRate ≈ 0.5; algo más pausado
        u.pitchMultiplier = 0.85f // tono más grave
        synth.speakUtterance(u)
        return true
    }

    override fun stop() {
        onDoneCb = null
        onStartCb = null
        synth.stopSpeakingAtBoundary(AVSpeechBoundaryImmediate)
    }

    override fun shutdown() = stop()
}
