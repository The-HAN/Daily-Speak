package com.thehan.dailyspeak.data.repository

import com.thehan.dailyspeak.data.local.dao.QuestionDao
import com.thehan.dailyspeak.data.local.entity.QuestionEntity
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.model.ReferenceAnswers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

    @Test
    fun keepsTheSameDailySetWithinOneDate() = runTest {
        val repository = OfflineFirstQuestionRepository(FakeQuestionDao())

        val first = repository.getDailyQuestions(
            date = "2026-09-19",
            count = 4,
            level = PracticeLevel.MIXED,
            topics = emptyList(),
        )
        val second = repository.getDailyQuestions(
            date = "2026-09-19",
            count = 4,
            level = PracticeLevel.MIXED,
            topics = emptyList(),
        )

        assertEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun changesTheDailySetOnTheNextDate() = runTest {
        val repository = OfflineFirstQuestionRepository(FakeQuestionDao())

        val first = repository.getDailyQuestions(
            date = "2026-09-19",
            count = 3,
            level = PracticeLevel.MIXED,
            topics = emptyList(),
        )
        val nextDay = repository.getDailyQuestions(
            date = "2026-09-20",
            count = 3,
            level = PracticeLevel.MIXED,
            topics = emptyList(),
        )

        assertTrue(first.map { it.id } != nextDay.map { it.id })
    }

    @Test
    fun cachesGeneratedQuestionsAndRetrievesThemById() = runTest {
        val dao = FakeQuestionDao()
        val repository = OfflineFirstQuestionRepository(dao)
        val generated = Question(
            id = "deepseek-2026-09-19-0",
            englishQuestion = "What is one habit that helps you study?",
            chineseMeaning = "哪一个习惯有助于你的学习？",
            topic = "教育与学习",
            difficulty = PracticeLevel.POSTGRADUATE,
            source = "deepseek",
            keyPhrases = listOf("keep me focused"),
            referenceAnswers = ReferenceAnswers(
                daily = "I make a plan.",
                advanced = "I make a short plan before studying.",
                postgraduate = "I make a short plan before studying, which keeps me focused.",
            ),
            createdAtEpochMillis = 1L,
        )

        repository.cacheQuestions(listOf(generated))

        val cached = repository.getQuestion(generated.id)
        assertNotNull(cached)
        assertEquals(generated, cached)
        assertEquals(listOf(generated.id), dao.upsertedQuestions.map { it.id })
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
