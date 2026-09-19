package com.thehan.dailyspeak.data.repository

import com.thehan.dailyspeak.data.local.dao.AttemptDao
import com.thehan.dailyspeak.data.local.dao.FavoriteDao
import com.thehan.dailyspeak.data.local.dao.FeedbackDao
import com.thehan.dailyspeak.data.local.dao.QuestionDao
import com.thehan.dailyspeak.data.local.entity.AttemptEntity
import com.thehan.dailyspeak.data.local.entity.FavoriteEntity
import com.thehan.dailyspeak.data.local.entity.FeedbackEntity
import com.thehan.dailyspeak.data.local.entity.QuestionEntity
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.ReferenceAnswers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomReviewRepositoryTest {
    @Test
    fun selectsLatestAttemptAndItsFeedback() = runTest {
        val repository = repository(
            questions = listOf(questionEntity(id = "q1")),
            attempts = listOf(
                attemptEntity(id = 1L, questionId = "q1", createdAtEpochMillis = 1_000L),
                attemptEntity(id = 2L, questionId = "q1", createdAtEpochMillis = 2_000L),
            ),
            feedback = listOf(
                feedbackEntity(attemptId = 1L, overall = 62f),
                feedbackEntity(attemptId = 2L, overall = 88f),
            ),
        )

        val item = repository.getReviewItems().single()

        assertEquals(2L, item.latestAttempt?.id)
        assertEquals(88f, item.feedback?.overall)
    }

    @Test
    fun includesFavoritedQuestionWithoutAttempt() = runTest {
        val repository = repository(
            questions = listOf(
                questionEntity(id = "answered"),
                questionEntity(id = "favorite-only"),
            ),
            attempts = listOf(
                attemptEntity(id = 7L, questionId = "answered", createdAtEpochMillis = 2_000L),
            ),
            favorites = listOf(FavoriteEntity(questionId = "favorite-only", createdAtEpochMillis = 3_000L)),
        )

        val item = repository.getReviewItems().single { it.question.id == "favorite-only" }

        assertTrue(item.isFavorite)
        assertNull(item.latestAttempt)
        assertNull(item.feedback)
    }

    @Test
    fun sortsByLatestAttemptThenFeedbackScore() = runTest {
        val repository = repository(
            questions = listOf(
                questionEntity(id = "older"),
                questionEntity(id = "newer-low"),
                questionEntity(id = "newer-high"),
            ),
            attempts = listOf(
                attemptEntity(id = 1L, questionId = "older", createdAtEpochMillis = 1_000L),
                attemptEntity(id = 2L, questionId = "newer-low", createdAtEpochMillis = 3_000L),
                attemptEntity(id = 3L, questionId = "newer-high", createdAtEpochMillis = 3_000L),
            ),
            feedback = listOf(
                feedbackEntity(attemptId = 1L, overall = 90f),
                feedbackEntity(attemptId = 2L, overall = 70f),
                feedbackEntity(attemptId = 3L, overall = 80f),
            ),
        )

        val ids = repository.getReviewItems().map { it.question.id }

        assertEquals(listOf("newer-high", "newer-low", "older"), ids)
    }

    private fun repository(
        questions: List<QuestionEntity>,
        attempts: List<AttemptEntity> = emptyList(),
        feedback: List<FeedbackEntity> = emptyList(),
        favorites: List<FavoriteEntity> = emptyList(),
    ): RoomReviewRepository = RoomReviewRepository(
        questionDao = FakeQuestionDao(questions),
        attemptDao = FakeAttemptDao(attempts),
        feedbackDao = FakeFeedbackDao(feedback),
        favoriteDao = FakeFavoriteDao(favorites),
    )

    private fun questionEntity(
        id: String,
        topic: String = "教育与学习",
        difficulty: PracticeLevel = PracticeLevel.DAILY,
    ): QuestionEntity = QuestionEntity(
        id = id,
        englishQuestion = "What do you think?",
        chineseMeaning = "你怎么看？",
        topic = topic,
        difficulty = difficulty,
        source = "test",
        keyPhrases = listOf("think"),
        referenceAnswers = ReferenceAnswers(
            daily = "I think...",
            advanced = "From my perspective...",
            postgraduate = "A balanced view suggests...",
        ),
        createdAtEpochMillis = 1L,
    )

    private fun attemptEntity(
        id: Long,
        questionId: String,
        createdAtEpochMillis: Long,
    ): AttemptEntity = AttemptEntity(
        id = id,
        questionId = questionId,
        audioPath = "/tmp/$id.m4a",
        transcript = "Test answer $id",
        durationMs = 2_000L,
        createdAtEpochMillis = createdAtEpochMillis,
    )

    private fun feedbackEntity(
        attemptId: Long,
        overall: Float,
    ): FeedbackEntity = FeedbackEntity(
        attemptId = attemptId,
        accuracy = overall,
        fluency = overall,
        completeness = overall,
        prosody = overall,
        overall = overall,
        pronunciationIssues = emptyList(),
        grammarSuggestions = emptyList(),
        vocabularySuggestions = emptyList(),
        logicSuggestions = emptyList(),
        betterAnswer = "Better answer",
        overallComment = "Comment",
        source = "test",
        createdAtEpochMillis = attemptId,
    )

    private class FakeQuestionDao(
        private val questions: List<QuestionEntity>,
    ) : QuestionDao {
        override suspend fun getAll(): List<QuestionEntity> = questions

        override suspend fun getById(id: String): QuestionEntity? =
            questions.firstOrNull { it.id == id }

        override suspend fun count(): Int = questions.size

        override suspend fun upsertAll(questions: List<QuestionEntity>) = Unit
    }

    private class FakeAttemptDao(
        private val attempts: List<AttemptEntity>,
    ) : AttemptDao {
        override suspend fun insert(attempt: AttemptEntity): Long = error("Not used")

        override suspend fun updateTranscript(id: Long, transcript: String) = Unit

        override suspend fun getById(id: Long): AttemptEntity? =
            attempts.firstOrNull { it.id == id }

        override suspend fun getAll(): List<AttemptEntity> = attempts

        override suspend fun getForQuestion(questionId: String): List<AttemptEntity> =
            attempts.filter { it.questionId == questionId }
    }

    private class FakeFeedbackDao(
        private val feedback: List<FeedbackEntity>,
    ) : FeedbackDao {
        override suspend fun upsert(feedback: FeedbackEntity) = Unit

        override suspend fun getByAttemptId(attemptId: Long): FeedbackEntity? =
            feedback.firstOrNull { it.attemptId == attemptId }

        override suspend fun getAll(): List<FeedbackEntity> = feedback
    }

    private class FakeFavoriteDao(
        private val favorites: List<FavoriteEntity>,
    ) : FavoriteDao {
        override suspend fun upsert(favorite: FavoriteEntity) = Unit

        override suspend fun delete(questionId: String) = Unit

        override suspend fun exists(questionId: String): Boolean =
            favorites.any { it.questionId == questionId }

        override suspend fun getAll(): List<FavoriteEntity> = favorites
    }
}