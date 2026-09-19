package com.thehan.dailyspeak.data.repository

import com.thehan.dailyspeak.data.local.dao.QuestionDao
import com.thehan.dailyspeak.data.local.seed.LocalQuestionBank
import com.thehan.dailyspeak.data.mapper.toDomain
import com.thehan.dailyspeak.data.mapper.toEntity
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.Question
import com.thehan.dailyspeak.domain.repository.QuestionRepository
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
        val startIndex = Math.floorMod(date.hashCode(), candidates.size)

        return List(minOf(requested, candidates.size)) { offset ->
            candidates[(startIndex + offset) % candidates.size]
        }
    }

    override suspend fun getQuestion(id: String): Question? =
        questionDao.getById(id)?.toDomain()

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

    private suspend fun seedIfNeeded() {
        if (questionDao.count() == 0) {
            questionDao.upsertAll(LocalQuestionBank.questions.map { it.toEntity() })
        }
    }

    private companion object {
        const val MAX_DAILY_COUNT = 20
    }
}
