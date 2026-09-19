package com.thehan.dailyspeak.domain.repository

import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.Question

interface QuestionRepository {
    /**
     * Returns a deterministic, date-based set of questions. Implementations must work offline
     * whenever cached or seeded questions are available.
     */
    suspend fun getDailyQuestions(
        date: String,
        count: Int,
        level: PracticeLevel,
        topics: List<String>,
    ): List<Question>

    suspend fun getQuestion(id: String): Question?

    /**
     * Persists generated or fetched questions so they remain available offline.
     */
    suspend fun cacheQuestions(questions: List<Question>)
}
