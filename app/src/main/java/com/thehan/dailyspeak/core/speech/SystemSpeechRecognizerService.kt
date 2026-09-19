package com.thehan.dailyspeak.core.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
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
) : SpeechRecognizerService {
    override suspend fun recognize(audioFile: File): String =
        recognizeDetailed(audioFile).transcript

    override suspend fun recognizeDetailed(audioFile: File): SpeechRecognitionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            throw UnsupportedOperationException("系统文件转写需要 Android 13 或更高版本。")
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            throw IllegalStateException("设备未安装可用的系统语音识别服务。")
        }

        val decoded = pcmAudioDecoder.decode(audioFile)
        return try {
            withContext(Dispatchers.Main.immediate) {
                recognizePcm(decoded)
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
    private suspend fun recognizePcm(decoded: DecodedPcmAudio): SpeechRecognitionResult =
        suspendCancellableCoroutine { continuation ->
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
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
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
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

    private fun errorMessage(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "系统无法读取录音，请重试。"
        SpeechRecognizer.ERROR_CLIENT -> "语音识别客户端出错。"
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
