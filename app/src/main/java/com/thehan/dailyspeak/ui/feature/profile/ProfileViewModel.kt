package com.thehan.dailyspeak.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thehan.dailyspeak.core.reminder.DailyReminderScheduler
import com.thehan.dailyspeak.core.speech.SpeechRecognizerCatalog
import com.thehan.dailyspeak.core.speech.SpeechRecognizerServiceInfo
import com.thehan.dailyspeak.domain.model.AccentPreference
import com.thehan.dailyspeak.domain.model.BackgroundPreset
import com.thehan.dailyspeak.domain.model.PracticeLevel
import com.thehan.dailyspeak.domain.model.SpeechRecognizerMode
import com.thehan.dailyspeak.domain.model.UserSettings
import com.thehan.dailyspeak.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val speechRecognizerServices: List<SpeechRecognizerServiceInfo> = emptyList(),
    val isOnDeviceRecognitionAvailable: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: DailyReminderScheduler,
    private val speechRecognizerCatalog: SpeechRecognizerCatalog,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        settings = settings,
                    )
                }
            }
        }
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                speechRecognizerCatalog.snapshot()
            }
            _uiState.update {
                it.copy(
                    speechRecognizerServices = snapshot.services,
                    isOnDeviceRecognitionAvailable = snapshot.isOnDeviceRecognitionAvailable,
                )
            }
        }
    }

    fun setDailyCount(count: Int) {
        viewModelScope.launch { settingsRepository.setDailyCount(count) }
    }

    fun setLevel(level: PracticeLevel) {
        viewModelScope.launch { settingsRepository.setLevel(level) }
    }

    fun setAccent(accent: AccentPreference) {
        viewModelScope.launch { settingsRepository.setAccent(accent) }
    }

    fun toggleTopic(topic: String) {
        val updatedTopics = _uiState.value.settings.topics.toMutableSet().apply {
            if (!add(topic)) remove(topic)
        }
        viewModelScope.launch { settingsRepository.setTopics(updatedTopics) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            if (enabled) {
                reminderScheduler.schedule(_uiState.value.settings.reminderTime)
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    fun setReminderTime(time: String) {
        viewModelScope.launch {
            settingsRepository.setReminderTime(time)
            if (_uiState.value.settings.reminderEnabled) {
                reminderScheduler.schedule(time)
            }
        }
    }

    fun saveDeepSeekApiKey(apiKey: String) {
        viewModelScope.launch { settingsRepository.setDeepSeekApiKey(apiKey) }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setTtsEnabled(enabled) }
    }

    fun setSpeechRecognizerMode(mode: SpeechRecognizerMode) {
        viewModelScope.launch { settingsRepository.setSpeechRecognizerMode(mode) }
    }

    fun selectSpeechRecognizerService(component: String) {
        viewModelScope.launch {
            settingsRepository.setSpeechRecognizerComponent(component)
            settingsRepository.setSpeechRecognizerMode(SpeechRecognizerMode.SELECTED_SERVICE)
        }
    }

    fun setBackgroundPreset(preset: BackgroundPreset) {
        viewModelScope.launch {
            settingsRepository.setBackgroundPreset(preset)
            settingsRepository.setBackgroundImageUri("")
        }
    }

    fun setBackgroundImageUri(uri: String) {
        viewModelScope.launch { settingsRepository.setBackgroundImageUri(uri) }
    }

    fun clearBackgroundImage() {
        viewModelScope.launch { settingsRepository.setBackgroundImageUri("") }
    }
}
