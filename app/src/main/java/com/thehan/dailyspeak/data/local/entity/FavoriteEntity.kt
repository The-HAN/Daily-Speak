package com.thehan.dailyspeak.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["questionId"], unique = true)],
)
data class FavoriteEntity(
    @PrimaryKey val questionId: String,
    val createdAtEpochMillis: Long,
)
