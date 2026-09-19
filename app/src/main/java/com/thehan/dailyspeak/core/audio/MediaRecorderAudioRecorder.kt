package com.thehan.dailyspeak.core.audio

import android.media.MediaRecorder
import android.os.SystemClock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRecorderAudioRecorder @Inject constructor() : AudioRecorder {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtElapsedMs: Long = 0L

    override val isRecording: Boolean
        get() = mediaRecorder != null

    @Suppress("DEPRECATION")
    override fun start(outputFile: File) {
        check(mediaRecorder == null) { "A recording is already in progress." }

        outputFile.parentFile?.mkdirs()
        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(16_000)
            setAudioChannels(1)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }

        this.mediaRecorder = recorder
        this.outputFile = outputFile
        startedAtElapsedMs = SystemClock.elapsedRealtime()
    }

    override fun stop(): Recording {
        val recorder = mediaRecorder ?: error("No recording is in progress.")
        val targetFile = outputFile ?: error("Recording output file is missing.")
        val durationMs = (SystemClock.elapsedRealtime() - startedAtElapsedMs).coerceAtLeast(0L)

        return try {
            recorder.stop()
            Recording(file = targetFile, durationMs = durationMs)
        } catch (error: RuntimeException) {
            targetFile.delete()
            throw IllegalStateException("录音时间太短或录音设备暂不可用。", error)
        } finally {
            release()
        }
    }

    override fun cancel() {
        val targetFile = outputFile
        runCatching { mediaRecorder?.stop() }
        release()
        targetFile?.delete()
    }

    private fun release() {
        runCatching { mediaRecorder?.reset() }
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
        outputFile = null
        startedAtElapsedMs = 0L
    }
}