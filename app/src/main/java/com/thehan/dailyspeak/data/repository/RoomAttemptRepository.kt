package com.thehan.dailyspeak.data.repository

import com.thehan.dailyspeak.data.local.dao.AttemptDao
import com.thehan.dailyspeak.data.mapper.toDomain
import com.thehan.dailyspeak.data.mapper.toEntity
import com.thehan.dailyspeak.domain.model.Attempt
import com.thehan.dailyspeak.domain.repository.AttemptRepository
import javax.inject.Inject

class RoomAttemptRepository @Inject constructor(
    private val attemptDao: AttemptDao,
) : AttemptRepository {
    override suspend fun save(attempt: Attempt): Long =
        attemptDao.insert(attempt.toEntity())

    override suspend fun updateTranscript(id: Long, transcript: String) {
        attemptDao.updateTranscript(id, transcript)
    }

    override suspend fun getById(id: Long): Attempt? =
        attemptDao.getById(id)?.toDomain()

    override suspend fun getForQuestion(questionId: String): List<Attempt> =
        attemptDao.getForQuestion(questionId).map { it.toDomain() }
}
