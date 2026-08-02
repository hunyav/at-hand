package com.athand.domain.model

import java.time.Instant
import java.time.LocalDate

data class PracticeDayKey(
    val practiceId: String,
    val dayIndex: Int
)

data class ReflectionEntry(
    val dayKey: PracticeDayKey,
    val date: LocalDate,
    val text: String,
    val updatedAt: Instant
)

data class PracticeCompletion(
    val dayKey: PracticeDayKey,
    val completedAt: Instant
)

data class PracticeProgress(
    val completedDays: Set<PracticeCompletion> = emptySet()
) {
    fun isCompleted(dayKey: PracticeDayKey): Boolean =
        completedDays.any { it.dayKey == dayKey }

    fun completedCountForPractice(practiceId: String): Int =
        completedDays.count { it.dayKey.practiceId == practiceId }
}
