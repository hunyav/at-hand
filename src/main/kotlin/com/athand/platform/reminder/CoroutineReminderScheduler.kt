package com.athand.platform.reminder

import com.athand.domain.model.UserPreferences
import com.athand.domain.service.ReminderScheduleCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

class CoroutineReminderScheduler(
    private val scope: CoroutineScope,
    private val onReminder: () -> Unit,
    private val calculator: ReminderScheduleCalculator = ReminderScheduleCalculator(),
    private val nowProvider: () -> LocalDateTime = { LocalDateTime.now() }
) {
    private var preferences: UserPreferences = UserPreferences()
    private var reminderJob: Job? = null

    fun updatePreferences(updatedPreferences: UserPreferences) {
        preferences = updatedPreferences
        reschedule()
    }

    fun stop() {
        reminderJob?.cancel()
        reminderJob = null
    }

    private fun reschedule() {
        reminderJob?.cancel()
        if (calculator.nextReminderTime(nowProvider(), preferences) == null) {
            reminderJob = null
            return
        }

        reminderJob = scope.launch {
            while (isActive) {
                val now = nowProvider()
                val next = calculator.nextReminderTime(now, preferences) ?: return@launch
                val delayMillis = Duration.between(now, next).toMillis().coerceAtLeast(0L)
                delay(delayMillis)
                if (!isActive) {
                    return@launch
                }
                onReminder()
            }
        }
    }
}
