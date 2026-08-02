package com.athand.presentation.history

import com.athand.domain.model.PracticeWeek
import com.athand.domain.model.ReflectionEntry

fun buildHistoryEntries(
    reflections: List<ReflectionEntry>,
    practices: List<PracticeWeek>
): List<HistoryEntryView> {
    if (reflections.isEmpty()) {
        return emptyList()
    }

    val practicesById = practices.associateBy { it.id }

    return reflections
        .asSequence()
        .filter { it.text.isNotBlank() }
        .sortedByDescending { it.updatedAt }
        .map { reflection ->
            val practice = practicesById[reflection.dayKey.practiceId]
            val dailyPractice = practice?.dailyPractices?.firstOrNull { it.dayIndex == reflection.dayKey.dayIndex }
            HistoryEntryView(
                id = "${reflection.dayKey.practiceId}-${reflection.dayKey.dayIndex}-${reflection.updatedAt.toEpochMilli()}",
                updatedLabel = reflection.date.toString(),
                practiceLabel = practice?.title ?: reflection.dayKey.practiceId,
                dayLabel = dailyPractice?.heading ?: "Day ${reflection.dayKey.dayIndex + 1}",
                text = reflection.text
            )
        }
        .toList()
}
