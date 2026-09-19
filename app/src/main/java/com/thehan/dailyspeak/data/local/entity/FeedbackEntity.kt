package com.thehan.dailyspeak.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "feedback",
    foreignKeys = [
        ForeignKey(
            entity = AttemptEntity::class,
            parentColumns = ["id"],
            childColumns = ["attemptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FeedbackEntity(
    @PrimaryKey val attemptId: Long,
    val accuracy: Float,
    val fluency: Float,
    val completeness: Float,
    val prosody: Float,
    val overall: Float,
    val pronunciationIssues: List<String>,
    val grammarSuggestions: List<String>,
    val vocabularySuggestions: List<String>,
    val logicSuggestions: List<String>,
    val naturalExpressionSuggestions: List<String>,
    val betterAnswer: String,
    val overallComment: String,
    val source: String,
    val createdAtEpochMillis: Long,
)
