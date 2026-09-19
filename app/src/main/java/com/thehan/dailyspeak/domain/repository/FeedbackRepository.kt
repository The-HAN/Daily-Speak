package com.thehan.dailyspeak.domain.repository

import com.thehan.dailyspeak.domain.model.Feedback

interface FeedbackRepository {
    suspend fun save(feedback: Feedback)

    suspend fun getByAttemptId(attemptId: Long): Feedback?
}
