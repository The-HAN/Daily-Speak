package com.thehan.dailyspeak.core.audio

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createRecordingFile(): File {
        val directory = File(context.filesDir, RECORDINGS_DIRECTORY).apply { mkdirs() }
        return File(directory, "attempt_${System.currentTimeMillis()}.m4a")
    }

    private companion object {
        const val RECORDINGS_DIRECTORY = "recordings"
    }
}