package com.thehan.dailyspeak

import android.app.Application
import com.thehan.dailyspeak.core.reminder.ReminderBootstrapper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DailySpeakApplication : Application() {
    @Inject lateinit var reminderBootstrapper: ReminderBootstrapper

    override fun onCreate() {
        super.onCreate()
        reminderBootstrapper.sync()
    }
}
