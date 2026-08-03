package com.athand.domain.model

import java.time.LocalTime

data class UserPreferences(
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null
)
