package com.thehan.dailyspeak.data.repository

import com.thehan.dailyspeak.data.local.dao.FeedbackDao
import com.thehan.dailyspeak.data.mapper.toDomain
import com.thehan.dailyspeak.data.mapper.toEntity
import com.thehan.dailyspeak.domain.model.Feedback
import com.thehan.dailyspeak.domain.repository.FeedbackRepository
import javax.inject.Inject

class RoomFeedbackRepository @Inject constructor(
    private val feedbackDao: FeedbackDao,
) : FeedbackRepository {
    override suspend fun save(feedback: Feedback) {
        feedbackDao.upsert(feedback.toEntity())
    }

    override suspend fun getByAttemptId(attemptId: Long): Feedback? =
        feedbackDao.getByAttemptId(attemptId)?.toDomain()
}
