package com.thehan.dailyspeak.data.repository

import com.thehan.dailyspeak.data.local.dao.QuestionDao
import com.thehan.dailyspeak.data.local.entity.QuestionEntity
import com.thehan.dailyspeak.domain.model.PracticeLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineFirstQuestionRepositoryTest {
    @Test
    fun seedsDatabaseAndReturnsRequestedCount() = runTest {
        val dao = FakeQuestionDao()
        val repository = OfflineFirstQuestionRepository(dao)

        val result = repository.getDailyQuestions(
            date = "2026-09-19",
            count = 3,
            level = PracticeLevel.MIXED,
            topics = emptyList(),
        )

        assertEquals(3, result.size)
        assertTrue(dao.upsertedQuestions.isNotEmpty())
    }

    @Test
    fun filtersBySelectedLevel() = runTest {
        val dao = FakeQuestionDao()
        val repository = OfflineFirstQuestionRepository(dao)

        val result = repository.getDailyQuestions(
            date = "2026-09-19",
            count = 20,
            level = PracticeLevel.POSTGRADUATE,
            topics = emptyList(),
        )

        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.difficulty == PracticeLevel.POSTGRADUATE })
    }

    @Test
    fun filtersByTopicWhenAvailable() = runTest {
        val dao = FakeQuestionDao()
        val repository = OfflineFirstQuestionRepository(dao)

        val result = repository.getDailyQuestions(
            date = "2026-09-19",
            count = 20,
            level = PracticeLevel.MIXED,
            topics = listOf("环境保护"),
        )

        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.topic == "环境保护" })
    }

    private class FakeQuestionDao : QuestionDao {
        private val questions = linkedMapOf<String, QuestionEntity>()
        val upsertedQuestions = mutableListOf<QuestionEntity>()

        override suspend fun getAll(): List<QuestionEntity> = questions.values.toList()

        override suspend fun count(): Int = questions.size

        override suspend fun getById(id: String): QuestionEntity? = questions[id]

        override suspend fun upsertAll(questions: List<QuestionEntity>) {
            upsertedQuestions += questions
            questions.forEach { this.questions[it.id] = it }
        }
    }
}