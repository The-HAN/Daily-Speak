package com.thehan.dailyspeak.core.di

import com.thehan.dailyspeak.data.repository.OfflineFirstQuestionRepository
import com.thehan.dailyspeak.data.repository.RoomAttemptRepository
import com.thehan.dailyspeak.data.repository.RoomFavoriteRepository
import com.thehan.dailyspeak.data.repository.RoomFeedbackRepository
import com.thehan.dailyspeak.domain.repository.AttemptRepository
import com.thehan.dailyspeak.domain.repository.FavoriteRepository
import com.thehan.dailyspeak.domain.repository.FeedbackRepository
import com.thehan.dailyspeak.domain.repository.QuestionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindQuestionRepository(
        repository: OfflineFirstQuestionRepository,
    ): QuestionRepository

    @Binds
    @Singleton
    abstract fun bindAttemptRepository(
        repository: RoomAttemptRepository,
    ): AttemptRepository

    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(
        repository: RoomFeedbackRepository,
    ): FeedbackRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(
        repository: RoomFavoriteRepository,
    ): FavoriteRepository
}
