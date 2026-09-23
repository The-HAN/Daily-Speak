package com.thehan.dailyspeak.data.repository

import com.thehan.dailyspeak.data.local.dao.QuestionDao
import com.thehan.dailyspeak.data.local.seed.LocalQuestionBank
import com.thehan.dailyspeak.data.mapper.toDomain
import com.thehan.dailyspeak.data.mapper.toEntity
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.repository.QuestionRepository
import java.time.LocalDate
import javax.inject.Inject

class OfflineFirstQuestionRepository @Inject constructor(
    private val questionDao: QuestionDao,
) : QuestionRepository {
    override suspend fun getDailyQuestions(
        date: String,
        count: Int,
        level: PracticeLevel,
        topics: List<String>,
    ): List<Question> {
        seedIfNeeded()

        val allQuestions = questionDao.getAll().map { it.toDomain() }
        if (allQuestions.isEmpty()) return emptyList()

        val candidates = selectCandidates(
            allQuestions = allQuestions,
            level = level,
            topics = topics,
        )
        val requested = count.coerceIn(1, MAX_DAILY_COUNT)
        return selectDailyQuestions(
            candidates = candidates,
            date = date,
            requested = requested,
        )
    }

    override suspend fun getQuestion(id: String): Question? =
        questionDao.getById(id)?.toDomain()

    override suspend fun cacheQuestions(questions: List<Question>) {
        if (questions.isEmpty()) return
        questionDao.upsertAll(questions.map { it.toEntity() })
    }

    private fun selectCandidates(
        allQuestions: List<Question>,
        level: PracticeLevel,
        topics: List<String>,
    ): List<Question> {
        val levelMatches = allQuestions.filter { question ->
            level == PracticeLevel.MIXED || question.difficulty == level
        }
        val topicMatches = if (topics.isEmpty()) {
            levelMatches
        } else {
            levelMatches.filter { it.topic in topics }
        }

        return topicMatches
            .ifEmpty { levelMatches }
            .ifEmpty { allQuestions }
    }

    /**
     * Randomizes the pool once, then rotates through it in non-overlapping daily blocks.
     * The same date/level/topic combination is stable, while consecutive days avoid merely
     * shifting the previous list by one item.
     */
    private fun selectDailyQuestions(
        candidates: List<Question>,
        date: String,
        requested: Int,
    ): List<Question> {
        if (candidates.isEmpty()) return emptyList()

        val shuffledPool = candidates.sortedWith(
            compareBy<Question> { it.id.stableOrderHash() }
                .thenBy { it.id },
        )
        val epochDay = runCatching { LocalDate.parse(date).toEpochDay() }
            .getOrElse { date.hashCode().toLong() }
        val rotation = epochDay * requested + if (requested >= shuffledPool.size) epochDay else 0L
        val startIndex = Math.floorMod(rotation, shuffledPool.size.toLong()).toInt()
        val resultSize = minOf(requested, shuffledPool.size)

        return List(resultSize) { offset ->
            shuffledPool[(startIndex + offset) % shuffledPool.size]
        }
    }

    private fun String.stableOrderHash(): Int {
        var hash = FNV_OFFSET_BASIS.toInt()
        for (character in this) {
            hash = (hash xor character.code) * FNV_PRIME
        }
        return hash
    }

    private suspend fun seedIfNeeded() {
        if (questionDao.count() == 0) {
            questionDao.upsertAll(LocalQuestionBank.questions.map { it.toEntity() })
        }
    }

    private companion object {
        const val MAX_DAILY_COUNT = 20
        const val FNV_OFFSET_BASIS = 0x811c9dc5L
        const val FNV_PRIME = 0x01000193
    }
}
