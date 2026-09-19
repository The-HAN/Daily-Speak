package com.thehan.dailyspeak.domain.service

import com.thehan.dailyspeak.domain.model.AccentPreference

interface TtsService {
    fun speak(text: String, accent: AccentPreference)

    fun stop()
}
