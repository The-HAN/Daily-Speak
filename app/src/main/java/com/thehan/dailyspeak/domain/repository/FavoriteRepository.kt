package com.thehan.dailyspeak.domain.repository

interface FavoriteRepository {
    suspend fun setFavorite(questionId: String, favorite: Boolean)

    suspend fun isFavorite(questionId: String): Boolean
}
