package com.thehan.dailyspeak.domain.service

import com.thehan.dailyspeak.domain.model.PronunciationScore
import java.io.File

interface PronunciationEvaluator {
    suspend fun evaluate(audioFile: File, expectedText: String): PronunciationScore
}

/**
 * Optional extension used by the default evaluator when recognition already
 * produced a transcript and confidence score.
 */
interface TranscriptAwarePronunciationEvaluator : PronunciationEvaluator {
    suspend fun evaluate(
        audioFile: File,
        expectedText: String,
        transcript: String,
        recognitionConfidence: Float?,
    ): PronunciationScore
}
