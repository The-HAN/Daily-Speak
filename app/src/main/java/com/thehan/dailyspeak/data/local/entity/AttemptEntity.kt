package com.thehan.dailyspeak.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["questionId"])],
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val questionId: String,
    val audioPath: String,
    val transcript: String,
    val durationMs: Long,
    val createdAtEpochMillis: Long,
)