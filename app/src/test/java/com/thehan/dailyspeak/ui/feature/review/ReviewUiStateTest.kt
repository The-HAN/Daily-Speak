package com.thehan.dailyspeak.ui.feature.review

import com.thehan.dailyspeak.domain.model.Attempt
import com.thehan.dailyspeak.domain.model.Feedback
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.model.ReferenceAnswers
import com.thehan.dailyspeak.domain.model.ReviewItem
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewUiStateTest {
    @Test
    fun lowScoreSectionOnlyReturnsFeedbackBelowThreshold() {
        val state = ReviewUiState(
            items = listOf(
                reviewItem(id = "low", overall = 69f),
                reviewItem(id = "passed", overall = 70f),
                reviewItem(id = "unscored"),
            ),
            selectedSection = ReviewSection.LOW_SCORE,
        )

        assertEquals(listOf("low"), state.filteredItems.map { it.question.id })
    }

    @Test
    fun favoriteSectionAppliesTopicAndDifficultyFilters() {
        val state = ReviewUiState(
            items = listOf(
                reviewItem(
                    id = "match",
                    difficulty = PracticeLevel.POSTGRADUATE,
                    topic = "教育",
                    favorite = true,
                ),
                reviewItem(
                    id = "wrong-topic",
                    difficulty = PracticeLevel.POSTGRADUATE,
                    topic = "科技",
                    favorite = true,
                ),
                reviewItem(
                    id = "wrong-difficulty",
                    difficulty = PracticeLevel.DAILY,
                    topic = "教育",
                    favorite = true,
                ),
            ),
            selectedSection = ReviewSection.FAVORITES,
            selectedDifficulty = PracticeLevel.POSTGRADUATE,
            selectedTopic = "教育",
        )

        assertEquals(listOf("match"), state.filteredItems.map { it.question.id })
    }

    @Test
    fun dateFilterExcludesOldAttempts() {
        val now = System.currentTimeMillis()
        val state = ReviewUiState(
            items = listOf(
                reviewItem(id = "recent", attemptCreatedAt = now),
                reviewItem(
                    id = "old",
                    attemptCreatedAt = Instant.now().minus(40, ChronoUnit.DAYS).toEpochMilli(),
                ),
            ),
            selectedSection = ReviewSection.HISTORY,
            selectedDateFilter = ReviewDateFilter.LAST_30_DAYS,
        )

        assertEquals(listOf("recent"), state.filteredItems.map { it.question.id })
    }

    private fun reviewItem(
        id: String,
        difficulty: PracticeLevel = PracticeLevel.DAILY,
        topic: String = "教育",
        favorite: Boolean = false,
        attemptCreatedAt: Long? = null,
        overall: Float? = null,
    ): ReviewItem {
        val attempt = attemptCreatedAt?.let {
            Attempt(
                id = id.hashCode().toLong(),
                questionId = id,
                audioPath = "/tmp/$id.m4a",
                transcript = "Answer $id",
                durationMs = 1_000L,
                createdAtEpochMillis = it,
            )
        }
        val feedback = overall?.let {
            Feedback(
                attemptId = attempt?.id ?: id.hashCode().toLong(),
                accuracy = it,
                fluency = it,
                completeness = it,
                prosody = it,
                overall = it,
                createdAtEpochMillis = attemptCreatedAt ?: 1L,
            )
        }
        return ReviewItem(
            question = Question(
                id = id,
                englishQuestion = "Question $id?",
                chineseMeaning = "问题 $id？",
                topic = topic,
                difficulty = difficulty,
                source = "test",
                keyPhrases = emptyList(),
                referenceAnswers = ReferenceAnswers(
                    daily = "Daily answer",
                    advanced = "Advanced answer",
                    postgraduate = "Postgraduate answer",
                ),
                createdAtEpochMillis = 1L,
            ),
            latestAttempt = attempt,
            feedback = feedback,
            isFavorite = favorite,
        )
    }
}