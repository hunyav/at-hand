package com.athand.domain.service

import com.athand.domain.model.UserPreferences
import java.time.LocalDateTime

class ReminderScheduleCalculator {
    fun nextReminderTime(now: LocalDateTime, preferences: UserPreferences): LocalDateTime? {
        if (!preferences.reminderEnabled) {
            return null
        }

        val reminderTime = preferences.reminderTime ?: return null
        val todayReminder = now.toLocalDate().atTime(reminderTime)
        return if (now.isBefore(todayReminder)) {
            todayReminder
        } else {
            todayReminder.plusDays(1)
        }
    }
}
