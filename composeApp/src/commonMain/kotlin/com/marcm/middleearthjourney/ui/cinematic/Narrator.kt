package com.marcm.middleearthjourney.ui.cinematic

import androidx.compose.runtime.Composable

/**
 * Narrador de voz (Text-to-Speech) multiplataforma.
 *  - Android (`actual`): `android.speech.tts.TextToSpeech` (voz masculina es-ES).
 *  - iOS (pendiente): `AVSpeechSynthesizer` + `AVSpeechUtterance`.
 */
interface Narrator {
    /** ¿El motor de voz está inicializado? */
    val isReady: Boolean

    /** Narra [text]. [onStart] al empezar la 1ª frase, [done] al terminar. Devuelve false si no hay voz. */
    fun speak(text: String, onStart: () -> Unit = {}, done: () -> Unit): Boolean

    fun stop()
    fun shutdown()
}

/** Crea/recuerda el narrador de la plataforma. */
@Composable
expect fun rememberNarrator(): Narrator
