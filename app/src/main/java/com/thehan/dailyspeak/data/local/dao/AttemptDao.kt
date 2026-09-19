package com.thehan.dailyspeak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.thehan.dailyspeak.data.local.entity.AttemptEntity

@Dao
interface AttemptDao {
    @Insert
    suspend fun insert(attempt: AttemptEntity): Long

    @Query("UPDATE attempts SET transcript = :transcript WHERE id = :id")
    suspend fun updateTranscript(id: Long, transcript: String)

    @Query("SELECT * FROM attempts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AttemptEntity?

    @Query("SELECT * FROM attempts ORDER BY createdAtEpochMillis DESC")
    suspend fun getAll(): List<AttemptEntity>

    @Query("SELECT * FROM attempts WHERE questionId = :questionId ORDER BY createdAtEpochMillis DESC")
    suspend fun getForQuestion(questionId: String): List<AttemptEntity>
}
