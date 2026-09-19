package com.thehan.dailyspeak.domain.service

import com.thehan.dailyspeak.domain.model.Feedback
import com.thehan.dailyspeak.domain.model.ImprovementSuggestion
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.PronunciationScore
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.model.ReferenceAnswers

/**
 * DeepSeek-backed generation and coaching contract.
 *
 * The concrete client reads the API key and model name at runtime. The model
 * name is deliberately not hard-coded because available DeepSeek models change
 * over time and must follow the official documentation.
 */
interface DeepSeekService {
    suspend fun generateDailyQuestions(
        date: String,
        count: Int,
        level: PracticeLevel,
        topics: List<String>,
    ): List<Question>

    suspend fun translateQuestion(question: Question): String

    suspend fun generateReferenceAnswers(
        question: Question,
        level: PracticeLevel,
    ): ReferenceAnswers

    suspend fun evaluateAnswer(
        question: Question,
        transcript: String,
        pronunciationScore: PronunciationScore,
    ): Feedback

    suspend fun improveAnswer(
        question: Question,
        transcript: String,
    ): ImprovementSuggestion
}
