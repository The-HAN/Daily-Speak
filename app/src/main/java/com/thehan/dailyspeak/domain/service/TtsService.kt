package com.thehan.dailyspeak.domain.service

import com.thehan.dailyspeak.domain.model.AccentPreference
import kotlinx.coroutines.flow.StateFlow

sealed interface TtsPlaybackState {
    data object Idle : TtsPlaybackState
    data object Initializing : TtsPlaybackState
    data object Speaking : TtsPlaybackState
    data class Error(val message: String) : TtsPlaybackState
}

interface TtsService {
    val playbackState: StateFlow<TtsPlaybackState>

    fun speak(text: String, accent: AccentPreference)

    fun stop()
}
