package com.thehan.dailyspeak.data.repository

import com.thehan.dailyspeak.data.local.dao.AttemptDao
import com.thehan.dailyspeak.data.local.dao.FavoriteDao
import com.thehan.dailyspeak.data.local.dao.FeedbackDao
import com.thehan.dailyspeak.data.local.dao.QuestionDao
import com.thehan.dailyspeak.data.mapper.toDomain
import com.thehan.dailyspeak.domain.model.Attempt
import com.thehan.dailyspeak.domain.model.Feedback
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.model.ReviewItem
import com.thehan.dailyspeak.domain.repository.ReviewRepository
import javax.inject.Inject

/**
 * Combines the small local data sets in memory. The local database is bounded
 * by daily practice and is expected to stay small; move this aggregation into a
 * SQL projection if history grows substantially.
 */
class RoomReviewRepository @Inject constructor(
    private val questionDao: QuestionDao,
    private val attemptDao: AttemptDao,
    private val feedbackDao: FeedbackDao,
    private val favoriteDao: FavoriteDao,
) : ReviewRepository {
    override suspend fun getReviewItems(): List<ReviewItem> {
        val questionEntities = questionDao.getAll()
        val attemptEntities = attemptDao.getAll()
        val feedbackEntities = feedbackDao.getAll()
        val favoriteEntities = favoriteDao.getAll()

        val latestAttempts = mutableMapOf<String, Attempt>()
        for (attemptEntity in attemptEntities) {
            val attempt: Attempt = attemptEntity.toDomain()
            val previous = latestAttempts[attempt.questionId]
            if (previous == null || attempt.createdAtEpochMillis > previous.createdAtEpochMillis) {
                latestAttempts[attempt.questionId] = attempt
            }
        }

        val feedbackByAttempt = mutableMapOf<Long, Feedback>()
        for (feedbackEntity in feedbackEntities) {
            val feedback: Feedback = feedbackEntity.toDomain()
            feedbackByAttempt[feedback.attemptId] = feedback
        }

        val favoriteIds = favoriteEntities.mapTo(mutableSetOf<String>()) { it.questionId }
        val result = mutableListOf<ReviewItem>()

        for (questionEntity in questionEntities) {
            val question: Question = questionEntity.toDomain()
            val latestAttempt: Attempt? = latestAttempts[question.id]
            val feedback: Feedback? = latestAttempt?.let { feedbackByAttempt[it.id] }
            result += ReviewItem(
                question = question,
                latestAttempt = latestAttempt,
                feedback = feedback,
                isFavorite = question.id in favoriteIds,
            )
        }

        result.sortWith(
            compareByDescending<ReviewItem> {
                it.latestAttempt?.createdAtEpochMillis ?: Long.MIN_VALUE
            }.thenByDescending { it.feedback?.overall ?: -1f },
        )
        return result
    }
}
