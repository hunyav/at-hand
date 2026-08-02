package com.athand.presentation.history

import com.athand.domain.model.DailyPractice
import com.athand.domain.model.PracticeDayKey
import com.athand.domain.model.PracticeWeek
import com.athand.domain.model.ReflectionEntry
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryEntryMapperTest {

    @Test
    fun `sorts by newest update and resolves practice labels`() {
        val practice = PracticeWeek(
            id = "control-001",
            title = "Steady Attention",
            principle = "Principle",
            overview = "Overview",
            dailyPractices = listOf(
                DailyPractice(dayIndex = 0, heading = "Learn", instruction = "Instruction")
            )
        )
        val reflections = listOf(
            ReflectionEntry(
                dayKey = PracticeDayKey(practiceId = "missing", dayIndex = 2),
                date = LocalDate.of(2026, 8, 1),
                text = "Fallback labels",
                updatedAt = Instant.parse("2026-08-01T10:00:00Z")
            ),
            ReflectionEntry(
                dayKey = PracticeDayKey(practiceId = "control-001", dayIndex = 0),
                date = LocalDate.of(2026, 8, 2),
                text = "Resolved labels",
                updatedAt = Instant.parse("2026-08-02T09:00:00Z")
            ),
            ReflectionEntry(
                dayKey = PracticeDayKey(practiceId = "control-001", dayIndex = 0),
                date = LocalDate.of(2026, 8, 2),
                text = "   ",
                updatedAt = Instant.parse("2026-08-02T12:00:00Z")
            )
        )

        val result = buildHistoryEntries(reflections = reflections, practices = listOf(practice))

        assertEquals(2, result.size)
        assertEquals("Resolved labels", result[0].text)
        assertEquals("Steady Attention", result[0].practiceLabel)
        assertEquals("Learn", result[0].dayLabel)
        assertEquals("Fallback labels", result[1].text)
        assertEquals("missing", result[1].practiceLabel)
        assertEquals("Day 3", result[1].dayLabel)
    }
}
