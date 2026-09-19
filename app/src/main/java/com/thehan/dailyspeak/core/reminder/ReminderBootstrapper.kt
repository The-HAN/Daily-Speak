package com.thehan.dailyspeak.core.reminder

import com.thehan.dailyspeak.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class ReminderBootstrapper @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val scheduler: DailyReminderScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun sync() {
        scope.launch { syncNow() }
    }

    suspend fun syncNow() {
        val settings = settingsRepository.settings.first()
        if (settings.reminderEnabled) {
            scheduler.schedule(settings.reminderTime)
        } else {
            scheduler.cancel()
        }
    }
}
