package com.athand.presentation.week

import com.athand.domain.model.DailyPractice
import com.athand.domain.model.PracticeCompletion
import com.athand.domain.model.PracticeDayKey
import com.athand.domain.model.PracticeProgress
import com.athand.domain.model.PracticeWeek
import com.athand.domain.model.ReflectionEntry
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WeekViewMapperTest {

    @Test
    fun `builds week day flags from practice, progress and reflections`() {
        val practice = testPractice()
        val progress = PracticeProgress(
            completedDays = setOf(
                PracticeCompletion(
                    dayKey = PracticeDayKey(practiceId = "control-001", dayIndex = 1),
                    completedAt = Instant.parse("2026-08-02T10:00:00Z")
                ),
                PracticeCompletion(
                    dayKey = PracticeDayKey(practiceId = "other", dayIndex = 0),
                    completedAt = Instant.parse("2026-08-02T10:00:00Z")
                )
            )
        )
        val reflections = listOf(
            ReflectionEntry(
                dayKey = PracticeDayKey(practiceId = "control-001", dayIndex = 0),
                date = LocalDate.of(2026, 8, 2),
                text = "Observed my reaction.",
                updatedAt = Instant.parse("2026-08-02T11:00:00Z")
            ),
            ReflectionEntry(
                dayKey = PracticeDayKey(practiceId = "control-001", dayIndex = 2),
                date = LocalDate.of(2026, 8, 2),
                text = "   ",
                updatedAt = Instant.parse("2026-08-02T11:05:00Z")
            )
        )

        val result = buildWeekDayViews(
            practice = practice,
            todayDayIndex = 1,
            progress = progress,
            reflections = reflections
        )

        assertEquals(listOf(0, 1, 2), result.map { it.dayIndex })
        assertTrue(result[0].hasReflection)
        assertTrue(result[1].isCompleted)
        assertTrue(result[1].isToday)
        assertTrue(result[2].isFuture)
        assertTrue(!result[2].hasReflection)
    }

    @Test
    fun `returns empty list when practice or day index is missing`() {
        val practice = testPractice()

        val noPractice = buildWeekDayViews(
            practice = null,
            todayDayIndex = 0,
            progress = PracticeProgress(),
            reflections = emptyList()
        )
        val noDayIndex = buildWeekDayViews(
            practice = practice,
            todayDayIndex = null,
            progress = PracticeProgress(),
            reflections = emptyList()
        )

        assertTrue(noPractice.isEmpty())
        assertTrue(noDayIndex.isEmpty())
    }

    private fun testPractice(): PracticeWeek = PracticeWeek(
        id = "control-001",
        title = "Steady Attention",
        principle = "Sort what is yours to direct from what is not.",
        overview = "Overview",
        dailyPractices = listOf(
            DailyPractice(dayIndex = 2, heading = "Wed", instruction = "Practice"),
            DailyPractice(dayIndex = 0, heading = "Mon", instruction = "Learn"),
            DailyPractice(dayIndex = 1, heading = "Tue", instruction = "Notice")
        )
    )
}
