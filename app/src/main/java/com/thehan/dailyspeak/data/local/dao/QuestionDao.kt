package com.thehan.dailyspeak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thehan.dailyspeak.data.local.entity.QuestionEntity

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY createdAtEpochMillis ASC, id ASC")
    suspend fun getAll(): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuestionEntity?

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(questions: List<QuestionEntity>)
}
