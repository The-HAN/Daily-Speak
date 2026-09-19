package com.thehan.dailyspeak.core.audio

import java.io.File

data class Recording(
    val file: File,
    val durationMs: Long,
)

interface AudioRecorder {
    val isRecording: Boolean

    fun start(outputFile: File)

    fun stop(): Recording

    fun cancel()
}