package com.thehan.dailyspeak.data.mapper

import com.thehan.dailyspeak.data.local.entity.QuestionEntity
import com.thehan.dailyspeak.domain.model.Question

fun QuestionEntity.toDomain(): Question = Question(
    id = id,
    englishQuestion = englishQuestion,
    chineseMeaning = chineseMeaning,
    topic = topic,
    difficulty = difficulty,
    source = source,
    keyPhrases = keyPhrases,
    referenceAnswers = referenceAnswers,
    createdAtEpochMillis = createdAtEpochMillis,
)

fun Question.toEntity(): QuestionEntity = QuestionEntity(
    id = id,
    englishQuestion = englishQuestion,
    chineseMeaning = chineseMeaning,
    topic = topic,
    difficulty = difficulty,
    source = source,
    keyPhrases = keyPhrases,
    referenceAnswers = referenceAnswers,
    createdAtEpochMillis = createdAtEpochMillis,
)