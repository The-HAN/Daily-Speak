package com.thehan.dailyspeak.domain.repository

import com.thehan.dailyspeak.domain.model.AccentPreference
import com.thehan.dailyspeak.domain.model.BackgroundPreset
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.SpeechRecognizerMode
import com.thehan.dailyspeak.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setDailyCount(count: Int)

    suspend fun setLevel(level: PracticeLevel)

    suspend fun setAccent(accent: AccentPreference)

    suspend fun setTopics(topics: Set<String>)

    suspend fun setReminderEnabled(enabled: Boolean)

    suspend fun setReminderTime(time: String)

    suspend fun setDeepSeekApiKey(apiKey: String)

    suspend fun setDeepSeekModel(model: String)

    suspend fun setTtsEnabled(enabled: Boolean)

    suspend fun setSpeechRecognizerMode(mode: SpeechRecognizerMode)

    suspend fun setSpeechRecognizerComponent(component: String)

    suspend fun setBackgroundPreset(preset: BackgroundPreset)

    suspend fun setBackgroundImageUri(uri: String)
}
