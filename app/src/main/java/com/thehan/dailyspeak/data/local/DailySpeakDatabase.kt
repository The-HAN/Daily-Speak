package com.thehan.dailyspeak.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.thehan.dailyspeak.data.local.dao.AttemptDao
import com.thehan.dailyspeak.data.local.dao.FavoriteDao
import com.thehan.dailyspeak.data.local.dao.FeedbackDao
import com.thehan.dailyspeak.data.local.dao.QuestionDao
import com.thehan.dailyspeak.data.local.entity.AttemptEntity
import com.thehan.dailyspeak.data.local.entity.FavoriteEntity
import com.thehan.dailyspeak.data.local.entity.FeedbackEntity
import com.thehan.dailyspeak.data.local.entity.QuestionEntity

@Database(
    entities = [
        QuestionEntity::class,
        AttemptEntity::class,
        FeedbackEntity::class,
        FavoriteEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class DailySpeakDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao

    abstract fun attemptDao(): AttemptDao

    abstract fun feedbackDao(): FeedbackDao

    abstract fun favoriteDao(): FavoriteDao
}
