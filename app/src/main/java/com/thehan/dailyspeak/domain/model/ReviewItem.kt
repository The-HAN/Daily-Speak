package com.thehan.dailyspeak.domain.model

data class ReviewItem(
    val question: Question,
    val latestAttempt: Attempt? = null,
    val feedback: Feedback? = null,
    val isFavorite: Boolean = false,
)