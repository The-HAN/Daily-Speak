package com.thehan.dailyspeak.domain.repository

import com.thehan.dailyspeak.domain.model.ReviewItem

interface ReviewRepository {
    suspend fun getReviewItems(): List<ReviewItem>
}