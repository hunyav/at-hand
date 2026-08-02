package com.athand.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.athand.data.practice.ResourcePracticeRepository
import com.athand.domain.service.TodayPracticeService
import com.athand.domain.time.SystemAppClock
import com.athand.presentation.theme.AtHandTheme
import com.athand.presentation.theme.ThemeMode
import com.athand.presentation.theme.next
import com.athand.presentation.today.TodayScreen
import java.time.LocalDate

private const val DEFAULT_REFLECTION_LABEL = "Reflection (not saved yet)"

fun main() = application {
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    val contentResult = remember { runCatching { loadTodayContent() } }

    Window(
        onCloseRequest = ::exitApplication,
        title = "At Hand",
        state = rememberWindowState(width = 460.dp, height = 620.dp),
        resizable = true
    ) {
        AtHandTheme(mode = themeMode) {
            val content = contentResult.getOrNull()
            TodayScreen(
                themeMode = themeMode,
                onThemeModeChange = { currentMode -> themeMode = currentMode.next() },
                onSettingsClick = {},
                weekTitle = content?.weekTitle ?: "Weekly Principle",
                principle = content?.principle ?: "Unable to load bundled practices.",
                todayHeading = content?.todayHeading ?: "Today",
                todayInstruction = content?.todayInstruction ?: "Please check bundled content and restart the app.",
                reflectionPrompt = content?.reflectionPrompt ?: DEFAULT_REFLECTION_LABEL,
                errorMessage = contentResult.exceptionOrNull()?.message
            )
        }
    }
}

private fun loadTodayContent(): TodayContent {
    val practices = ResourcePracticeRepository().loadPracticeWeeks()
    val today = TodayPracticeService(
        clock = SystemAppClock(),
        sequenceStartDate = LocalDate.of(2026, 1, 5)
    ).selectTodayPractice(practices)

    return TodayContent(
        weekTitle = today.practice.title,
        principle = today.practice.principle,
        todayHeading = today.dailyPractice.heading,
        todayInstruction = today.dailyPractice.instruction,
        reflectionPrompt = today.dailyPractice.reflectionPrompt ?: DEFAULT_REFLECTION_LABEL
    )
}

private data class TodayContent(
    val weekTitle: String,
    val principle: String,
    val todayHeading: String,
    val todayInstruction: String,
    val reflectionPrompt: String
)
