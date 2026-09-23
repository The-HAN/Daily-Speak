package com.thehan.dailyspeak.domain.model

data class UserSettings(
    val dailyCount: Int = DEFAULT_DAILY_COUNT,
    val level: PracticeLevel = PracticeLevel.MIXED,
    val accent: AccentPreference = AccentPreference.AMERICAN,
    val topics: Set<String> = emptySet(),
    val reminderEnabled: Boolean = false,
    val reminderTime: String = DEFAULT_REMINDER_TIME,
    val deepSeekApiKey: String = "",
    val deepSeekModel: String = "",
    val ttsEnabled: Boolean = true,
    val speechRecognizerMode: SpeechRecognizerMode = SpeechRecognizerMode.SYSTEM_DEFAULT,
    val speechRecognizerComponent: String = "",
    val backgroundPreset: BackgroundPreset = BackgroundPreset.DEFAULT,
    val backgroundImageUri: String = "",
) {
    companion object {
        const val DEFAULT_DAILY_COUNT = 5
        const val MIN_DAILY_COUNT = 1
        const val MAX_DAILY_COUNT = 20
        const val DEFAULT_REMINDER_TIME = "20:00"
    }
}
