package com.thehan.dailyspeak.core.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionService
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import com.thehan.dailyspeak.domain.model.AccentPreference
import com.thehan.dailyspeak.domain.model.SpeechRecognizerMode
import com.thehan.dailyspeak.domain.repository.SettingsRepository
import com.thehan.dailyspeak.domain.service.SpeechRecognitionResult
import com.thehan.dailyspeak.domain.service.SpeechRecognizerService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Android 13+ file-source adapter.
 *
 * EXTRA_AUDIO_SOURCE is a platform contract, but individual recognizer
 * services may still reject it. This implementation reports that limitation
 * instead of fabricating a transcript.
 */
@Singleton
class SystemSpeechRecognizerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pcmAudioDecoder: PcmAudioDecoder,
    private val settingsRepository: SettingsRepository,
) : SpeechRecognizerService {
    override suspend fun recognize(audioFile: File): String =
        recognizeDetailed(audioFile).transcript

    override suspend fun recognizeDetailed(audioFile: File): SpeechRecognitionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            throw UnsupportedOperationException("系统文件转写需要 Android 13 或更高版本。")
        }

        val settings = settingsRepository.settings.first()
        requireRecognitionAvailable(
            mode = settings.speechRecognizerMode,
            selectedComponent = settings.speechRecognizerComponent,
        )
        val locale = when (settings.accent) {
            AccentPreference.AMERICAN -> Locale.US
            AccentPreference.BRITISH -> Locale.UK
        }

        val decoded = pcmAudioDecoder.decode(audioFile)
        return try {
            withContext(Dispatchers.Main.immediate) {
                recognizePcm(
                    decoded = decoded,
                    mode = settings.speechRecognizerMode,
                    selectedComponent = settings.speechRecognizerComponent,
                    locale = locale,
                )
            }
        } catch (error: Throwable) {
            throw IllegalStateException(
                "系统语音识别失败：${error.message ?: "未知错误"}",
                error,
            )
        } finally {
            decoded.file.delete()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun recognizePcm(
        decoded: DecodedPcmAudio,
        mode: SpeechRecognizerMode,
        selectedComponent: String,
        locale: Locale,
    ): SpeechRecognitionResult =
        suspendCancellableCoroutine { continuation ->
            val recognizer = createRecognizer(mode, selectedComponent)
            val audioSource = ParcelFileDescriptor.open(
                decoded.file,
                ParcelFileDescriptor.MODE_READ_ONLY,
            )
            var completed = false

            fun destroy() {
                runCatching { recognizer.destroy() }
                runCatching { audioSource.close() }
            }

            fun complete(result: Result<SpeechRecognitionResult>) {
                if (completed) return
                completed = true
                destroy()
                result.fold(
                    onSuccess = { value -> continuation.resume(value) },
                    onFailure = { error -> continuation.resumeWithException(error) },
                )
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    complete(Result.failure(IllegalStateException(errorMessage(error))))
                }

                override fun onResults(results: Bundle?) {
                    val transcripts = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    val transcript = transcripts.firstOrNull().orEmpty().trim()
                    if (transcript.isBlank()) {
                        complete(Result.failure(IllegalStateException("没有识别到清晰语音，请靠近麦克风后重试。")))
                        return
                    }

                    val confidence = results
                        ?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                        ?.firstOrNull()
                        ?.takeIf { it.isFinite() && it >= 0f }

                    complete(
                        Result.success(
                            SpeechRecognitionResult(
                                transcript = transcript,
                                confidence = confidence,
                            ),
                        ),
                    )
                }

                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audioSource)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, decoded.channelCount)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, decoded.pcmEncoding)
                putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, decoded.sampleRateHz)
            }

            continuation.invokeOnCancellation {
                Handler(Looper.getMainLooper()).post {
                    if (!completed) {
                        runCatching { recognizer.cancel() }
                        complete(Result.failure(IllegalStateException("语音识别已取消。")))
                    }
                }
            }

            try {
                recognizer.startListening(intent)
            } catch (error: Throwable) {
                complete(Result.failure(error))
            }
        }

    private fun requireRecognitionAvailable(
        mode: SpeechRecognizerMode,
        selectedComponent: String,
    ) {
        when (mode) {
            SpeechRecognizerMode.SYSTEM_DEFAULT -> {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    throw IllegalStateException(
                        "设备未安装可用的系统语音识别服务，请在“我的 → 语音识别服务”中检查。",
                    )
                }
            }

            SpeechRecognizerMode.SELECTED_SERVICE -> {
                val component = selectedComponent.toComponentName()
                    ?: throw IllegalStateException(
                        "还没有选择可用的语音识别服务，请先在“我的 → 语音识别服务”中设置。",
                    )
                val serviceIntent = Intent(RecognitionService.SERVICE_INTERFACE)
                    .setComponent(component)
                val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.resolveService(
                        serviceIntent,
                        PackageManager.ResolveInfoFlags.of(0L),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.resolveService(serviceIntent, 0)
                }
                if (resolved == null) {
                    throw IllegalStateException(
                        "所选语音识别服务当前不可用，请重新选择“系统默认”或其他服务。",
                    )
                }
            }

            SpeechRecognizerMode.ON_DEVICE -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    throw UnsupportedOperationException("设备端识别需要 Android 12 或更高版本。")
                }
                if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    throw IllegalStateException(
                        "当前系统没有可用的设备端识别能力，请切换回系统默认识别。",
                    )
                }
            }
        }
    }

    private fun createRecognizer(
        mode: SpeechRecognizerMode,
        selectedComponent: String,
    ): SpeechRecognizer = when (mode) {
        SpeechRecognizerMode.SYSTEM_DEFAULT -> SpeechRecognizer.createSpeechRecognizer(context)
        SpeechRecognizerMode.SELECTED_SERVICE -> {
            val component = selectedComponent.toComponentName()
                ?: throw IllegalStateException("所选语音识别服务信息无效。")
            SpeechRecognizer.createSpeechRecognizer(context, component)
        }

        SpeechRecognizerMode.ON_DEVICE -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                throw UnsupportedOperationException("设备端识别需要 Android 12 或更高版本。")
            }
            createOnDeviceRecognizer()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createOnDeviceRecognizer(): SpeechRecognizer =
        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)

    private fun String.toComponentName(): ComponentName? =
        takeIf(String::isNotBlank)?.let(ComponentName::unflattenFromString)

    private fun errorMessage(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "系统无法读取录音，请重试。"
        SpeechRecognizer.ERROR_CLIENT -> "语音识别客户端出错；所选服务可能不支持对已有录音文件转写，请切换其他识别服务后重试。"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少麦克风权限。"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        -> "系统识别服务网络异常，请检查网络后重试。"
        SpeechRecognizer.ERROR_NO_MATCH -> "没有识别到匹配的英文回答，请重试。"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "系统语音识别服务正忙，请稍后重试。"
        SpeechRecognizer.ERROR_SERVER -> "系统语音识别服务暂时不可用。"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到语音，请靠近麦克风后重试。"
        else -> "系统语音识别失败，错误码：$errorCode。"
    }
}
