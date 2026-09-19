package com.thehan.dailyspeak.data.mapper

import com.thehan.dailyspeak.data.local.entity.AttemptEntity
import com.thehan.dailyspeak.domain.model.Attempt

fun Attempt.toEntity(): AttemptEntity = AttemptEntity(
    id = id,
    questionId = questionId,
    audioPath = audioPath,
    transcript = transcript,
    durationMs = durationMs,
    createdAtEpochMillis = createdAtEpochMillis,
)

fun AttemptEntity.toDomain(): Attempt = Attempt(
    id = id,
    questionId = questionId,
    audioPath = audioPath,
    transcript = transcript,
    durationMs = durationMs,
    createdAtEpochMillis = createdAtEpochMillis,
)