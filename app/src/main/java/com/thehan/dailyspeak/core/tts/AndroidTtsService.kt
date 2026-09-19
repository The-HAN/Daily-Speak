package com.thehan.dailyspeak.core.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import com.thehan.dailyspeak.domain.model.AccentPreference
import com.thehan.dailyspeak.domain.service.TtsService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTtsService @Inject constructor(
    @ApplicationContext context: Context,
) : TtsService {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var ready = false
    private var pendingSpeech: (() -> Unit)? = null

    private val textToSpeech = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            val action = pendingSpeech
            pendingSpeech = null
            action?.invoke()
        } else {
            pendingSpeech = null
        }
    }

    override fun speak(text: String, accent: AccentPreference) {
        if (text.isBlank()) return
        mainHandler.post {
            val action: () -> Unit = {
                val locale = when (accent) {
                    AccentPreference.AMERICAN -> Locale.US
                    AccentPreference.BRITISH -> Locale.UK
                }
                val availability = textToSpeech.isLanguageAvailable(locale)
                if (availability >= TextToSpeech.LANG_AVAILABLE) {
                    textToSpeech.language = locale
                }
                textToSpeech.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    UTTERANCE_ID,
                )
            }

            if (ready) {
                action()
            } else {
                pendingSpeech = action
            }
        }
    }

    override fun stop() {
        mainHandler.post {
            pendingSpeech = null
            runCatching { textToSpeech.stop() }
        }
    }

    private companion object {
        const val UTTERANCE_ID = "daily_speak_tts"
    }
}
