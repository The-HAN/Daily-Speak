package com.thehan.dailyspeak.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.ReferenceAnswers

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["difficulty"]),
        Index(value = ["topic"]),
    ],
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val englishQuestion: String,
    val chineseMeaning: String,
    val topic: String,
    val difficulty: PracticeLevel,
    val source: String,
    val keyPhrases: List<String>,
    val referenceAnswers: ReferenceAnswers,
    val createdAtEpochMillis: Long,
)