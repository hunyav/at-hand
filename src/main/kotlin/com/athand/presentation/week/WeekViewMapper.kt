package com.athand.presentation.week

import com.athand.domain.model.PracticeProgress
import com.athand.domain.model.PracticeWeek
import com.athand.domain.model.ReflectionEntry

fun buildWeekDayViews(
    practice: PracticeWeek?,
    todayDayIndex: Int?,
    progress: PracticeProgress,
    reflections: List<ReflectionEntry>
): List<WeekDayView> {
    if (practice == null || todayDayIndex == null) {
        return emptyList()
    }

    val completedDays = progress.completedDays
        .asSequence()
        .filter { it.dayKey.practiceId == practice.id }
        .map { it.dayKey.dayIndex }
        .toSet()

    val reflectedDays = reflections
        .asSequence()
        .filter { it.dayKey.practiceId == practice.id }
        .filter { it.text.isNotBlank() }
        .map { it.dayKey.dayIndex }
        .toSet()

    return practice.dailyPractices
        .sortedBy { it.dayIndex }
        .map { day ->
            WeekDayView(
                dayIndex = day.dayIndex,
                heading = day.heading,
                instruction = day.instruction,
                isCompleted = day.dayIndex in completedDays,
                hasReflection = day.dayIndex in reflectedDays,
                isToday = day.dayIndex == todayDayIndex,
                isFuture = day.dayIndex > todayDayIndex
            )
        }
}
