package com.thehan.dailyspeak.domain.repository

import com.thehan.dailyspeak.domain.model.Attempt

interface AttemptRepository {
    suspend fun save(attempt: Attempt): Long

    suspend fun updateTranscript(id: Long, transcript: String)

    suspend fun getById(id: Long): Attempt?

    suspend fun getForQuestion(questionId: String): List<Attempt>
}
