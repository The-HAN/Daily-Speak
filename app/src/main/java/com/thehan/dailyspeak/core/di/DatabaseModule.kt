package com.thehan.dailyspeak.core.di

import android.content.Context
import androidx.room.Room
import com.thehan.dailyspeak.data.local.DailySpeakDatabase
import com.thehan.dailyspeak.data.local.dao.AttemptDao
import com.thehan.dailyspeak.data.local.dao.FavoriteDao
import com.thehan.dailyspeak.data.local.dao.FeedbackDao
import com.thehan.dailyspeak.data.local.dao.QuestionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): DailySpeakDatabase = Room.databaseBuilder(
        context,
        DailySpeakDatabase::class.java,
        "daily_speak.db",
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    @Provides
    fun provideQuestionDao(database: DailySpeakDatabase): QuestionDao = database.questionDao()

    @Provides
    fun provideAttemptDao(database: DailySpeakDatabase): AttemptDao = database.attemptDao()

    @Provides
    fun provideFeedbackDao(database: DailySpeakDatabase): FeedbackDao = database.feedbackDao()

    @Provides
    fun provideFavoriteDao(database: DailySpeakDatabase): FavoriteDao = database.favoriteDao()
}
