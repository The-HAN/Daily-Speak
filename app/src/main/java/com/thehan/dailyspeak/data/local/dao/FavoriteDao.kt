package com.thehan.dailyspeak.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.thehan.dailyspeak.data.local.entity.FavoriteEntity

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE questionId = :questionId")
    suspend fun delete(questionId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE questionId = :questionId)")
    suspend fun exists(questionId: String): Boolean

    @Query("SELECT * FROM favorites ORDER BY createdAtEpochMillis DESC")
    suspend fun getAll(): List<FavoriteEntity>
}
