package com.thehan.dailyspeak.core.di

import com.thehan.dailyspeak.core.audio.AudioRecorder
import com.thehan.dailyspeak.core.audio.MediaRecorderAudioRecorder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    @Binds
    @Singleton
    abstract fun bindAudioRecorder(
        recorder: MediaRecorderAudioRecorder,
    ): AudioRecorder
}