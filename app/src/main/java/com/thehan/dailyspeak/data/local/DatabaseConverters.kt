package com.thehan.dailyspeak.data.local

import androidx.room.TypeConverter
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.ReferenceAnswers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DatabaseConverters {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())

    @TypeConverter
    fun fromReferenceAnswers(value: ReferenceAnswers): String = json.encodeToString(value)

    @TypeConverter
    fun toReferenceAnswers(value: String): ReferenceAnswers =
        json.decodeFromString(value)

    @TypeConverter
    fun fromPracticeLevel(value: PracticeLevel): String = value.name

    @TypeConverter
    fun toPracticeLevel(value: String): PracticeLevel =
        PracticeLevel.entries.firstOrNull { it.name == value } ?: PracticeLevel.DAILY
}