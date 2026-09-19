package com.thehan.dailyspeak.core.di

import com.thehan.dailyspeak.core.speech.DefaultPronunciationEvaluator
import com.thehan.dailyspeak.core.speech.SystemSpeechRecognizerService
import com.thehan.dailyspeak.core.tts.AndroidTtsService
import com.thehan.dailyspeak.domain.service.PronunciationEvaluator
import com.thehan.dailyspeak.domain.service.SpeechRecognizerService
import com.thehan.dailyspeak.domain.service.TtsService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SpeechModule {
    @Binds
    @Singleton
    abstract fun bindSpeechRecognizerService(
        service: SystemSpeechRecognizerService,
    ): SpeechRecognizerService

    @Binds
    @Singleton
    abstract fun bindPronunciationEvaluator(
        evaluator: DefaultPronunciationEvaluator,
    ): PronunciationEvaluator

    @Binds
    @Singleton
    abstract fun bindTtsService(
        service: AndroidTtsService,
    ): TtsService
}
