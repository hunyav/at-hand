package com.athand.domain.service

import com.athand.domain.model.DailyPractice
import com.athand.domain.model.PracticeWeek
import com.athand.domain.time.AppClock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class TodayPracticeSelection(
    val practice: PracticeWeek,
    val dailyPractice: DailyPractice,
    val dayIndex: Int,
    val elapsedWeeks: Long
)

class TodayPracticeService(
    private val clock: AppClock,
    private val sequenceStartDate: LocalDate
) {
    fun selectTodayPractice(practices: List<PracticeWeek>): TodayPracticeSelection {
        require(practices.isNotEmpty()) { "Practices list must not be empty" }

        val today = clock.today()
        val elapsedWeeks = elapsedWeeks(today)
        val practice = practices[(elapsedWeeks % practices.size).toInt()]

        val dayIndex = today.dayOfWeek.value - DayOfWeek.MONDAY.value
        val dailyPractice = practice.dailyPractices.firstOrNull { it.dayIndex == dayIndex }
            ?: throw IllegalStateException("Practice '${practice.id}' does not define day index $dayIndex")

        return TodayPracticeSelection(
            practice = practice,
            dailyPractice = dailyPractice,
            dayIndex = dayIndex,
            elapsedWeeks = elapsedWeeks
        )
    }

    private fun elapsedWeeks(today: LocalDate): Long {
        val startWeek = sequenceStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return ChronoUnit.WEEKS.between(startWeek, currentWeek).coerceAtLeast(0)
    }
}
