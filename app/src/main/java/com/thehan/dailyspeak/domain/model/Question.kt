package com.thehan.dailyspeak.domain.model

data class Question(
    val id: String,
    val englishQuestion: String,
    val chineseMeaning: String,
    val topic: String,
    val difficulty: PracticeLevel,
    val source: String,
    val keyPhrases: List<String>,
    val referenceAnswers: ReferenceAnswers,
    val createdAtEpochMillis: Long,
)