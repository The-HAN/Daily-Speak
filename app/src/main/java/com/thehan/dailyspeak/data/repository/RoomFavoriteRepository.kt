package com.thehan.dailyspeak.data.repository

import com.thehan.dailyspeak.data.local.dao.FavoriteDao
import com.thehan.dailyspeak.data.local.entity.FavoriteEntity
import com.thehan.dailyspeak.domain.repository.FavoriteRepository
import javax.inject.Inject

class RoomFavoriteRepository @Inject constructor(
    private val favoriteDao: FavoriteDao,
) : FavoriteRepository {
    override suspend fun setFavorite(questionId: String, favorite: Boolean) {
        if (favorite) {
            favoriteDao.upsert(
                FavoriteEntity(
                    questionId = questionId,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        } else {
            favoriteDao.delete(questionId)
        }
    }

    override suspend fun isFavorite(questionId: String): Boolean = favoriteDao.exists(questionId)
}
