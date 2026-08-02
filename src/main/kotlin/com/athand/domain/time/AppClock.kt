package com.athand.domain.time

import java.time.Instant
import java.time.LocalDate

interface AppClock {
    fun today(): LocalDate
    fun now(): Instant
}

class SystemAppClock : AppClock {
    override fun today(): LocalDate = LocalDate.now()

    override fun now(): Instant = Instant.now()
}
