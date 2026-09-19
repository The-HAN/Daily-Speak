package com.thehan.dailyspeak.domain.service

import java.io.File

data class SpeechRecognitionResult(
    val transcript: String,
    val confidence: Float? = null,
)

interface SpeechRecognizerService {
    suspend fun recognize(audioFile: File): String

    /**
     * Detailed variant used by the practice pipeline. Implementations that do
     * not expose confidence may keep the default implementation.
     */
    suspend fun recognizeDetailed(audioFile: File): SpeechRecognitionResult =
        SpeechRecognitionResult(transcript = recognize(audioFile))
}
