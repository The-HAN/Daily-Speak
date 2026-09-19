package com.thehan.dailyspeak.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReferenceAnswers(
    val daily: String,
    val advanced: String,
    val postgraduate: String,
)