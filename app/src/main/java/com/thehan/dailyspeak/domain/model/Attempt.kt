package com.thehan.dailyspeak.domain.model

data class Attempt(
    val id: Long = 0L,
    val questionId: String,
    val audioPath: String,
    val transcript: String,
    val durationMs: Long,
    val createdAtEpochMillis: Long,
)