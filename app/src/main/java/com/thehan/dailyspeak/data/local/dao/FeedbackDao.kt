package com.thehan.dailyspeak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thehan.dailyspeak.data.local.entity.FeedbackEntity

@Dao
interface FeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(feedback: FeedbackEntity)

    @Query("SELECT * FROM feedback WHERE attemptId = :attemptId LIMIT 1")
    suspend fun getByAttemptId(attemptId: Long): FeedbackEntity?

    @Query("SELECT * FROM feedback ORDER BY createdAtEpochMillis DESC")
    suspend fun getAll(): List<FeedbackEntity>
}
