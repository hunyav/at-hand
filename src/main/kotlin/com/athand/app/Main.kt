package com.athand.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.athand.data.practice.ResourcePracticeRepository
import com.athand.data.progress.JsonProgressRepository
import com.athand.data.reflection.JsonReflectionRepository
import com.athand.domain.model.PracticeDayKey
import com.athand.domain.model.PracticeProgress
import com.athand.domain.model.ReflectionEntry
import com.athand.domain.repository.ProgressRepository
import com.athand.domain.repository.ReflectionRepository
import com.athand.domain.repository.RepositoryLoadResult
import com.athand.domain.service.TodayPracticeService
import com.athand.domain.time.AppClock
import com.athand.domain.time.SystemAppClock
import com.athand.platform.DefaultAppDirectories
import com.athand.presentation.theme.AtHandTheme
import com.athand.presentation.theme.ThemeMode
import com.athand.presentation.theme.next
import com.athand.presentation.today.TodayScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val DEFAULT_REFLECTION_LABEL = "Reflection (not saved yet)"
private val SEQUENCE_START_DATE: LocalDate = LocalDate.of(2026, 1, 5)

fun main() = application {
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var uiState by remember { mutableStateOf(TodayUiState()) }

    val scope = rememberCoroutineScope()
    val clock = remember { SystemAppClock() }
    val appDirectories = remember { DefaultAppDirectories() }
    val reflectionRepository = remember { JsonReflectionRepository(appDirectories) }
    val progressRepository = remember { JsonProgressRepository(appDirectories) }

    LaunchedEffect(Unit) {
        uiState = loadInitialUiState(
            clock = clock,
            reflectionRepository = reflectionRepository,
            progressRepository = progressRepository
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "At Hand",
        state = rememberWindowState(width = 460.dp, height = 620.dp),
        resizable = true
    ) {
        AtHandTheme(mode = themeMode) {
            val content = uiState.content
            TodayScreen(
                themeMode = themeMode,
                onThemeModeChange = { currentMode -> themeMode = currentMode.next() },
                onSettingsClick = {},
                weekTitle = content?.weekTitle ?: "Weekly Principle",
                principle = content?.principle ?: "Unable to load bundled practices",
                todayHeading = content?.todayHeading ?: "Today",
                todayInstruction = content?.todayInstruction ?: "Please check bundled content and restart the app.",
                reflectionPrompt = content?.reflectionPrompt ?: DEFAULT_REFLECTION_LABEL,
                reflectionText = uiState.reflectionText,
                onReflectionTextChange = { updatedText ->
                    uiState = uiState.copy(reflectionText = updatedText, statusMessage = null)
                },
                completed = uiState.completed,
                onCompletedChange = { updatedCompleted ->
                    uiState = uiState.copy(completed = updatedCompleted, statusMessage = null)
                },
                onSaveClick = onSaveClick@{
                    val currentContent = uiState.content ?: return@onSaveClick
                    val currentStateSnapshot = uiState
                    scope.launch {
                        uiState = uiState.copy(isSaving = true, statusMessage = null)
                        uiState = saveTodayState(
                            currentState = currentStateSnapshot,
                            todayContent = currentContent,
                            clock = clock,
                            reflectionRepository = reflectionRepository,
                            progressRepository = progressRepository
                        )
                    }
                },
                weekProgressLabel = if (content != null) {
                    "Week progress: ${uiState.completedCount}/7 complete"
                } else {
                    "Week progress: --"
                },
                isLoading = uiState.isLoading,
                isSaving = uiState.isSaving,
                statusMessage = uiState.statusMessage,
                errorMessage = uiState.errorMessage
            )
        }
    }
}

private suspend fun loadInitialUiState(
    clock: AppClock,
    reflectionRepository: ReflectionRepository,
    progressRepository: ProgressRepository
): TodayUiState {
    val content = runCatching { loadTodayContent(clock) }
        .getOrElse { error ->
            return TodayUiState(
                isLoading = false,
                errorMessage = error.message ?: "Unable to load today's practice"
            )
        }

    val reflectionLoad = runCatching { reflectionRepository.loadEntries() }
        .getOrElse { error ->
            RepositoryLoadResult(
                data = emptyList(),
                warning = "Could not load reflections: ${error.message ?: "unknown error"}"
            )
        }
    val progressLoad = runCatching { progressRepository.loadProgress() }
        .getOrElse { error ->
            RepositoryLoadResult(
                data = PracticeProgress(),
                warning = "Could not load completion state: ${error.message ?: "unknown error"}"
            )
        }
    val todayKey = PracticeDayKey(practiceId = content.practiceId, dayIndex = content.dayIndex)

    val reflectionText = reflectionLoad.data
        .firstOrNull { it.dayKey == todayKey }
        ?.text
        .orEmpty()

    val warningMessage = listOfNotNull(reflectionLoad.warning, progressLoad.warning)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(separator = "\n")

    return TodayUiState(
        isLoading = false,
        content = content,
        reflectionText = reflectionText,
        completed = progressLoad.data.isCompleted(todayKey),
        completedCount = progressLoad.data.completedCountForPractice(content.practiceId),
        errorMessage = warningMessage
    )
}

private suspend fun saveTodayState(
    currentState: TodayUiState,
    todayContent: TodayContent,
    clock: AppClock,
    reflectionRepository: ReflectionRepository,
    progressRepository: ProgressRepository
): TodayUiState {
    return runCatching {
        val dayKey = PracticeDayKey(
            practiceId = todayContent.practiceId,
            dayIndex = todayContent.dayIndex
        )
        val now = clock.now()

        reflectionRepository.upsert(
            ReflectionEntry(
                dayKey = dayKey,
                date = clock.today(),
                text = currentState.reflectionText,
                updatedAt = now
            )
        )
        progressRepository.setCompleted(dayKey = dayKey, completed = currentState.completed, completedAt = now)

        val reloadedProgress = runCatching { progressRepository.loadProgress().data }
            .getOrElse { error ->
                return@runCatching currentState.copy(
                    isLoading = false,
                    isSaving = false,
                    statusMessage = "Saved locally",
                    errorMessage = "Saved, but could not reload progress: ${error.message ?: "unknown error"}"
                )
            }

        currentState.copy(
            isLoading = false,
            isSaving = false,
            completedCount = reloadedProgress.completedCountForPractice(todayContent.practiceId),
            statusMessage = "Saved locally",
            errorMessage = null
        )
    }.getOrElse { error ->
        currentState.copy(
            isLoading = false,
            isSaving = false,
            statusMessage = null,
            errorMessage = "Could not save today's practice: ${error.message ?: "unknown error"}"
        )
    }
}

private fun loadTodayContent(clock: AppClock): TodayContent {
    val practices = ResourcePracticeRepository().loadPracticeWeeks()
    val today = TodayPracticeService(
        clock = clock,
        sequenceStartDate = SEQUENCE_START_DATE
    ).selectTodayPractice(practices)

    return TodayContent(
        practiceId = today.practice.id,
        dayIndex = today.dayIndex,
        weekTitle = today.practice.title,
        principle = today.practice.principle,
        todayHeading = today.dailyPractice.heading,
        todayInstruction = today.dailyPractice.instruction,
        reflectionPrompt = today.dailyPractice.reflectionPrompt ?: DEFAULT_REFLECTION_LABEL
    )
}

private data class TodayUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val content: TodayContent? = null,
    val reflectionText: String = "",
    val completed: Boolean = false,
    val completedCount: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private data class TodayContent(
    val practiceId: String,
    val dayIndex: Int,
    val weekTitle: String,
    val principle: String,
    val todayHeading: String,
    val todayInstruction: String,
    val reflectionPrompt: String
)
