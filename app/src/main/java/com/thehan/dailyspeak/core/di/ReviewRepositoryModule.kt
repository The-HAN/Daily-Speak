package com.thehan.dailyspeak.core.di

import com.thehan.dailyspeak.data.repository.RoomReviewRepository
import com.thehan.dailyspeak.domain.repository.ReviewRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReviewRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindReviewRepository(
        repository: RoomReviewRepository,
    ): ReviewRepository
}