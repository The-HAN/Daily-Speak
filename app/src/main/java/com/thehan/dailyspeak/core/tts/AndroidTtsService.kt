package com.thehan.dailyspeak.core.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.thehan.dailyspeak.domain.model.AccentPreference
import com.thehan.dailyspeak.domain.service.TtsPlaybackState
import com.thehan.dailyspeak.domain.service.TtsService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AndroidTtsService @Inject constructor(
    @ApplicationContext private val context: Context,
) : TtsService {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _playbackState = MutableStateFlow<TtsPlaybackState>(TtsPlaybackState.Idle)
    override val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    private var textToSpeech: TextToSpeech? = null
    private var initializationStarted = false
    private var ready = false
    private var pendingSpeech: PendingSpeech? = null
    private var currentUtteranceId: String? = null

    override fun speak(text: String, accent: AccentPreference) {
        if (text.isBlank()) return
        mainHandler.post { speakOnMain(text, accent) }
    }

    private fun speakOnMain(text: String, accent: AccentPreference) {
        if (ready) {
            startSpeech(text, accent)
            return
        }

        pendingSpeech = PendingSpeech(text = text, accent = accent)
        if (_playbackState.value is TtsPlaybackState.Error) {
            resetEngine()
        }
        ensureInitialized()
    }

    private fun ensureInitialized() {
        if (initializationStarted) return
        initializationStarted = true
        _playbackState.value = TtsPlaybackState.Initializing

        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            onInitialized(engine, status)
        }
        textToSpeech = engine
    }

    private fun onInitialized(engine: TextToSpeech, status: Int) {
        if (textToSpeech !== engine) {
            runCatching { engine.shutdown() }
            return
        }

        if (status != TextToSpeech.SUCCESS) {
            ready = false
            pendingSpeech = null
            _playbackState.value = TtsPlaybackState.Error(
                "系统 TTS 初始化失败，请确认设备已安装并启用文字转语音服务。",
            )
            return
        }

        runCatching {
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
        }
        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (utteranceId == currentUtteranceId) {
                        _playbackState.value = TtsPlaybackState.Speaking
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == currentUtteranceId) {
                        currentUtteranceId = null
                        _playbackState.value = TtsPlaybackState.Idle
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    handleSpeechError(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    handleSpeechError(utteranceId)
                }
            },
        )

        ready = true
        _playbackState.value = TtsPlaybackState.Idle
        pendingSpeech?.let { speech ->
            pendingSpeech = null
            startSpeech(speech.text, speech.accent)
        }
    }

    private fun handleSpeechError(utteranceId: String?) {
        if (utteranceId != currentUtteranceId) return
        currentUtteranceId = null
        _playbackState.value = TtsPlaybackState.Error(
            "系统 TTS 朗读失败，请检查媒体音量和系统 TTS 语音数据。",
        )
    }

    private fun startSpeech(text: String, accent: AccentPreference) {
        val engine = textToSpeech
        if (engine == null || !ready) {
            pendingSpeech = PendingSpeech(text = text, accent = accent)
            ensureInitialized()
            return
        }

        val locale = when (accent) {
            AccentPreference.AMERICAN -> Locale.US
            AccentPreference.BRITISH -> Locale.UK
        }
        val availability = runCatching { engine.isLanguageAvailable(locale) }.getOrElse {
            _playbackState.value = TtsPlaybackState.Error(
                "无法读取系统 TTS 语言状态：${it.message ?: "未知错误"}。",
            )
            return
        }
        if (availability < TextToSpeech.LANG_AVAILABLE) {
            _playbackState.value = TtsPlaybackState.Error(
                "系统 TTS 缺少英语语音数据，请到系统设置中安装或启用英语语音包。",
            )
            return
        }

        val languageResult = runCatching { engine.setLanguage(locale) }.getOrElse {
            _playbackState.value = TtsPlaybackState.Error(
                "无法切换系统 TTS 英语语音：${it.message ?: "未知错误"}。",
            )
            return
        }
        if (languageResult < TextToSpeech.LANG_AVAILABLE) {
            _playbackState.value = TtsPlaybackState.Error(
                "系统 TTS 缺少英语语音数据，请到系统设置中安装或启用英语语音包。",
            )
            return
        }

        runCatching { engine.setSpeechRate(0.92f) }
        runCatching { engine.setPitch(1f) }

        val utteranceId = "$UTTERANCE_ID_PREFIX-${System.nanoTime()}"
        currentUtteranceId = utteranceId
        val result = runCatching {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }.getOrElse { error ->
            currentUtteranceId = null
            _playbackState.value = TtsPlaybackState.Error(
                "系统 TTS 启动失败：${error.message ?: "未知错误"}。",
            )
            return
        }
        if (result == TextToSpeech.ERROR) {
            currentUtteranceId = null
            _playbackState.value = TtsPlaybackState.Error(
                "系统 TTS 无法播放当前内容，请检查系统 TTS 设置。",
            )
        } else {
            _playbackState.value = TtsPlaybackState.Speaking
        }
    }

    private fun resetEngine() {
        ready = false
        initializationStarted = false
        currentUtteranceId = null
        runCatching { textToSpeech?.shutdown() }
        textToSpeech = null
    }

    override fun stop() {
        mainHandler.post {
            pendingSpeech = null
            currentUtteranceId = null
            runCatching { textToSpeech?.stop() }
            if (_playbackState.value !is TtsPlaybackState.Error) {
                _playbackState.value = TtsPlaybackState.Idle
            }
        }
    }

    private data class PendingSpeech(
        val text: String,
        val accent: AccentPreference,
    )

    private companion object {
        const val UTTERANCE_ID_PREFIX = "daily_speak_tts"
    }
}
