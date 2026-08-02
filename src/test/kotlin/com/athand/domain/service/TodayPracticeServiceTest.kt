package com.athand.domain.service

import com.athand.domain.model.DailyPractice
import com.athand.domain.model.PracticeWeek
import com.athand.domain.time.AppClock
import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Instant
import java.time.LocalDate

class TodayPracticeServiceTest {

    @Test
    fun `selects Monday as day index zero`() {
        val service = TodayPracticeService(
            clock = FakeClock(today = LocalDate.of(2026, 1, 5)),
            sequenceStartDate = LocalDate.of(2026, 1, 5)
        )

        val selection = service.selectTodayPractice(samplePractices())

        assertEquals("week-1", selection.practice.id)
        assertEquals(0, selection.dayIndex)
        assertEquals("week-1-day-0", selection.dailyPractice.heading)
    }

    @Test
    fun `selects Sunday as day index six`() {
        val service = TodayPracticeService(
            clock = FakeClock(today = LocalDate.of(2026, 1, 11)),
            sequenceStartDate = LocalDate.of(2026, 1, 5)
        )

        val selection = service.selectTodayPractice(samplePractices())

        assertEquals("week-1", selection.practice.id)
        assertEquals(6, selection.dayIndex)
        assertEquals("week-1-day-6", selection.dailyPractice.heading)
    }

    @Test
    fun `advances practice on next Monday`() {
        val service = TodayPracticeService(
            clock = FakeClock(today = LocalDate.of(2026, 1, 12)),
            sequenceStartDate = LocalDate.of(2026, 1, 5)
        )

        val selection = service.selectTodayPractice(samplePractices())

        assertEquals("week-2", selection.practice.id)
        assertEquals(0, selection.dayIndex)
        assertEquals(1, selection.elapsedWeeks)
    }

    @Test
    fun `wraps after multiple weeks`() {
        val service = TodayPracticeService(
            clock = FakeClock(today = LocalDate.of(2026, 1, 26)),
            sequenceStartDate = LocalDate.of(2026, 1, 5)
        )

        val selection = service.selectTodayPractice(samplePractices())

        assertEquals(3, selection.elapsedWeeks)
        assertEquals("week-1", selection.practice.id)
    }

    @Test
    fun `dates before start week clamp to first practice`() {
        val service = TodayPracticeService(
            clock = FakeClock(today = LocalDate.of(2025, 12, 31)),
            sequenceStartDate = LocalDate.of(2026, 1, 5)
        )

        val selection = service.selectTodayPractice(samplePractices())

        assertEquals(0, selection.elapsedWeeks)
        assertEquals("week-1", selection.practice.id)
    }

    private fun samplePractices(): List<PracticeWeek> = listOf(
        sampleWeek("week-1"),
        sampleWeek("week-2"),
        sampleWeek("week-3")
    )

    private fun sampleWeek(id: String): PracticeWeek {
        val days = (0..6).map { dayIndex ->
            DailyPractice(
                dayIndex = dayIndex,
                heading = "$id-day-$dayIndex",
                instruction = "instruction-$dayIndex"
            )
        }
        return PracticeWeek(
            id = id,
            title = "Title $id",
            principle = "Principle $id",
            overview = "Overview $id",
            dailyPractices = days
        )
    }

    private class FakeClock(private val today: LocalDate) : AppClock {
        override fun today(): LocalDate = today

        override fun now(): Instant = Instant.EPOCH
    }
}
