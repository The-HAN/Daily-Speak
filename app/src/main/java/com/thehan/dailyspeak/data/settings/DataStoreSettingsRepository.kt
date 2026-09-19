package com.thehan.dailyspeak.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.thehan.dailyspeak.domain.model.AccentPreference
import com.thehan.dailyspeak.domain.model.BackgroundPreset
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.SpeechRecognizerMode
import com.thehan.dailyspeak.domain.model.UserSettings
import com.thehan.dailyspeak.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val settings: Flow<UserSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            UserSettings(
                dailyCount = preferences[Keys.DAILY_COUNT]
                    ?.coerceIn(UserSettings.MIN_DAILY_COUNT, UserSettings.MAX_DAILY_COUNT)
                    ?: UserSettings.DEFAULT_DAILY_COUNT,
                level = preferences[Keys.LEVEL]
                    ?.let(::practiceLevelOrNull)
                    ?: PracticeLevel.MIXED,
                accent = preferences[Keys.ACCENT]
                    ?.let(::accentOrNull)
                    ?: AccentPreference.AMERICAN,
                topics = preferences[Keys.TOPICS].orEmpty(),
                reminderEnabled = preferences[Keys.REMINDER_ENABLED] ?: false,
                reminderTime = preferences[Keys.REMINDER_TIME]
                    ?.takeIf(REMINDER_TIME_REGEX::matches)
                    ?: UserSettings.DEFAULT_REMINDER_TIME,
                deepSeekApiKey = preferences[Keys.DEEPSEEK_API_KEY].orEmpty(),
                ttsEnabled = preferences[Keys.TTS_ENABLED] ?: true,
                speechRecognizerMode = preferences[Keys.SPEECH_RECOGNIZER_MODE]
                    ?.let(::speechRecognizerModeOrNull)
                    ?: SpeechRecognizerMode.SYSTEM_DEFAULT,
                speechRecognizerComponent = preferences[Keys.SPEECH_RECOGNIZER_COMPONENT].orEmpty(),
                backgroundPreset = preferences[Keys.BACKGROUND_PRESET]
                    ?.let(::backgroundPresetOrNull)
                    ?: BackgroundPreset.DEFAULT,
                backgroundImageUri = preferences[Keys.BACKGROUND_IMAGE_URI].orEmpty(),
            )
        }

    override suspend fun setDailyCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.DAILY_COUNT] = count.coerceIn(
                UserSettings.MIN_DAILY_COUNT,
                UserSettings.MAX_DAILY_COUNT,
            )
        }
    }

    override suspend fun setLevel(level: PracticeLevel) {
        dataStore.edit { it[Keys.LEVEL] = level.name }
    }

    override suspend fun setAccent(accent: AccentPreference) {
        dataStore.edit { it[Keys.ACCENT] = accent.name }
    }

    override suspend fun setTopics(topics: Set<String>) {
        dataStore.edit { it[Keys.TOPICS] = topics }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.REMINDER_ENABLED] = enabled }
    }

    override suspend fun setReminderTime(time: String) {
        if (!REMINDER_TIME_REGEX.matches(time)) return
        dataStore.edit { it[Keys.REMINDER_TIME] = time }
    }

    override suspend fun setDeepSeekApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            val normalized = apiKey.trim()
            if (normalized.isEmpty()) {
                preferences.remove(Keys.DEEPSEEK_API_KEY)
            } else {
                preferences[Keys.DEEPSEEK_API_KEY] = normalized
            }
        }
    }

    override suspend fun setTtsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.TTS_ENABLED] = enabled }
    }

    override suspend fun setSpeechRecognizerMode(mode: SpeechRecognizerMode) {
        dataStore.edit { it[Keys.SPEECH_RECOGNIZER_MODE] = mode.name }
    }

    override suspend fun setSpeechRecognizerComponent(component: String) {
        dataStore.edit { preferences ->
            val normalized = component.trim()
            if (normalized.isEmpty()) {
                preferences.remove(Keys.SPEECH_RECOGNIZER_COMPONENT)
            } else {
                preferences[Keys.SPEECH_RECOGNIZER_COMPONENT] = normalized
            }
        }
    }

    override suspend fun setBackgroundPreset(preset: BackgroundPreset) {
        dataStore.edit { it[Keys.BACKGROUND_PRESET] = preset.name }
    }

    override suspend fun setBackgroundImageUri(uri: String) {
        dataStore.edit { preferences ->
            val normalized = uri.trim()
            if (normalized.isEmpty()) {
                preferences.remove(Keys.BACKGROUND_IMAGE_URI)
            } else {
                preferences[Keys.BACKGROUND_IMAGE_URI] = normalized
            }
        }
    }

    private fun practiceLevelOrNull(value: String): PracticeLevel? =
        PracticeLevel.entries.firstOrNull { it.name == value }

    private fun accentOrNull(value: String): AccentPreference? =
        AccentPreference.entries.firstOrNull { it.name == value }

    private fun speechRecognizerModeOrNull(value: String): SpeechRecognizerMode? =
        SpeechRecognizerMode.entries.firstOrNull { it.name == value }

    private fun backgroundPresetOrNull(value: String): BackgroundPreset? =
        BackgroundPreset.entries.firstOrNull { it.name == value }

    private object Keys {
        val DAILY_COUNT = intPreferencesKey("daily_count")
        val LEVEL = stringPreferencesKey("practice_level")
        val ACCENT = stringPreferencesKey("accent")
        val TOPICS = stringSetPreferencesKey("topics")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_TIME = stringPreferencesKey("reminder_time")
        val DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val SPEECH_RECOGNIZER_MODE = stringPreferencesKey("speech_recognizer_mode")
        val SPEECH_RECOGNIZER_COMPONENT = stringPreferencesKey("speech_recognizer_component")
        val BACKGROUND_PRESET = stringPreferencesKey("background_preset")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
    }

    private companion object {
        val REMINDER_TIME_REGEX = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")
    }
}
