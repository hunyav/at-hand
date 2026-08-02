package com.athand.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.athand.data.practice.ResourcePracticeRepository
import com.athand.data.progress.JsonProgressRepository
import com.athand.data.reflection.JsonReflectionRepository
import com.athand.domain.model.DailyPractice
import com.athand.domain.model.PracticeDayKey
import com.athand.domain.model.PracticeWeek
import com.athand.domain.model.PracticeProgress
import com.athand.domain.model.ReflectionEntry
import com.athand.domain.repository.ProgressRepository
import com.athand.domain.repository.ReflectionRepository
import com.athand.domain.repository.RepositoryLoadResult
import com.athand.domain.service.TodayPracticeService
import com.athand.domain.time.AppClock
import com.athand.domain.time.SystemAppClock
import com.athand.platform.DefaultAppDirectories
import com.athand.presentation.history.HistoryScreen
import com.athand.presentation.history.buildHistoryEntries
import com.athand.presentation.settings.SettingsScreen
import com.athand.presentation.theme.AtHandTheme
import com.athand.presentation.theme.ThemeMode
import com.athand.presentation.theme.next
import com.athand.presentation.today.TodayScreen
import com.athand.presentation.week.WeekScreen
import com.athand.presentation.week.buildWeekDayViews
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val DEFAULT_REFLECTION_LABEL = "Reflection (not saved yet)"
private val SEQUENCE_START_DATE: LocalDate = LocalDate.of(2026, 1, 5)

fun main() = application {
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    var currentDestination by remember { mutableStateOf(TopLevelDestination.TODAY) }
    var uiState by remember { mutableStateOf(AppUiState()) }

    val scope = rememberCoroutineScope()
    val clock = remember { SystemAppClock() }
    val appDirectories = remember { DefaultAppDirectories() }
    val reflectionRepository = remember { JsonReflectionRepository(appDirectories) }
    val progressRepository = remember { JsonProgressRepository(appDirectories) }
    val dataDirectoryPath = remember { appDirectories.appDataDirectory().toString() }

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
            AppLayout(
                themeMode = themeMode,
                onThemeModeChange = { currentMode -> themeMode = currentMode.next() },
                currentDestination = currentDestination,
                onDestinationSelected = { destination -> currentDestination = destination },
                uiState = uiState,
                onReflectionTextChange = { updatedText ->
                    uiState = uiState.copy(reflectionText = updatedText, statusMessage = null)
                },
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
                dataDirectoryPath = dataDirectoryPath,
                errorMessage = uiState.errorMessage
            )
        }
    }
}

@Composable
private fun AppLayout(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    currentDestination: TopLevelDestination,
    onDestinationSelected: (TopLevelDestination) -> Unit,
    uiState: AppUiState,
    onReflectionTextChange: (String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    dataDirectoryPath: String,
    errorMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeaderRow(
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange
        )

        DestinationRow(
            currentDestination = currentDestination,
            onDestinationSelected = onDestinationSelected
        )

        val content = uiState.content
        when (currentDestination) {
            TopLevelDestination.TODAY -> TodayScreen(
                weekTitle = content?.practice?.title ?: "Weekly Principle",
                principle = content?.practice?.principle ?: "Unable to load bundled practices",
                todayHeading = content?.dailyPractice?.heading ?: "Today",
                todayInstruction = content?.dailyPractice?.instruction
                    ?: "Please check bundled content and restart the app.",
                reflectionPrompt = content?.dailyPractice?.reflectionPrompt ?: DEFAULT_REFLECTION_LABEL,
                reflectionText = uiState.reflectionText,
                onReflectionTextChange = onReflectionTextChange,
                completed = uiState.completed,
                onCompletedChange = onCompletedChange,
                onSaveClick = onSaveClick,
                weekProgressLabel = if (content != null) {
                    "Week progress: ${uiState.completedCount}/7 complete"
                } else {
                    "Week progress: --"
                },
                isLoading = uiState.isLoading,
                isSaving = uiState.isSaving,
                statusMessage = uiState.statusMessage,
                errorMessage = errorMessage
            )

            TopLevelDestination.WEEK -> WeekScreen(
                weekTitle = content?.practice?.title ?: "Current week",
                principle = content?.practice?.principle ?: "",
                days = buildWeekDayViews(
                    practice = content?.practice,
                    todayDayIndex = content?.dayIndex,
                    progress = uiState.progress,
                    reflections = uiState.reflections
                ),
                isLoading = uiState.isLoading,
                errorMessage = errorMessage
            )

            TopLevelDestination.HISTORY -> HistoryScreen(
                entries = buildHistoryEntries(
                    reflections = uiState.reflections,
                    practices = uiState.practiceWeeks
                ),
                isLoading = uiState.isLoading,
                errorMessage = errorMessage
            )

            TopLevelDestination.SETTINGS -> SettingsScreen(
                themeLabel = "${themeMode.label} (use the button above to switch)",
                dataDirectoryPath = dataDirectoryPath
            )
        }
    }
}

@Composable
private fun HeaderRow(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "At Hand",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Button(onClick = { onThemeModeChange(themeMode) }) {
            Text(text = "Theme: ${themeMode.label}")
        }
    }
}

@Composable
private fun DestinationRow(
    currentDestination: TopLevelDestination,
    onDestinationSelected: (TopLevelDestination) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TopLevelDestination.entries.forEach { destination ->
            if (destination == currentDestination) {
                Button(onClick = { onDestinationSelected(destination) }) {
                    Text(destination.label)
                }
            } else {
                TextButton(onClick = { onDestinationSelected(destination) }) {
                    Text(destination.label)
                }
            }
        }
    }
}

private suspend fun loadInitialUiState(
    clock: AppClock,
    reflectionRepository: ReflectionRepository,
    progressRepository: ProgressRepository
): AppUiState {
    val content = runCatching { loadPracticeContext(clock) }
        .getOrElse { error ->
            return AppUiState(
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
    val todayKey = PracticeDayKey(
        practiceId = content.today.practice.id,
        dayIndex = content.today.dayIndex
    )

    val reflectionText = reflectionLoad.data
        .firstOrNull { it.dayKey == todayKey }
        ?.text
        .orEmpty()

    val warningMessage = listOfNotNull(reflectionLoad.warning, progressLoad.warning)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(separator = "\n")

    return AppUiState(
        isLoading = false,
        practiceWeeks = content.practiceWeeks,
        content = content.today,
        reflections = reflectionLoad.data,
        progress = progressLoad.data,
        reflectionText = reflectionText,
        completed = progressLoad.data.isCompleted(todayKey),
        completedCount = progressLoad.data.completedCountForPractice(content.today.practice.id),
        errorMessage = warningMessage
    )
}

private suspend fun saveTodayState(
    currentState: AppUiState,
    todayContent: TodayContent,
    clock: AppClock,
    reflectionRepository: ReflectionRepository,
    progressRepository: ProgressRepository
): AppUiState {
    return runCatching {
        val dayKey = PracticeDayKey(
            practiceId = todayContent.practice.id,
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

        val reloadedReflections = runCatching { reflectionRepository.loadEntries() }
            .getOrElse { error ->
                RepositoryLoadResult(
                    data = currentState.reflections,
                    warning = "Saved, but could not reload reflections: ${error.message ?: "unknown error"}"
                )
            }

        val reloadedProgress = runCatching { progressRepository.loadProgress() }
            .getOrElse { error ->
                RepositoryLoadResult(
                    data = currentState.progress,
                    warning = "Saved, but could not reload progress: ${error.message ?: "unknown error"}"
                )
            }

        val warningMessage = listOfNotNull(reloadedReflections.warning, reloadedProgress.warning)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = "\n")

        currentState.copy(
            isLoading = false,
            isSaving = false,
            reflections = reloadedReflections.data,
            progress = reloadedProgress.data,
            completedCount = reloadedProgress.data.completedCountForPractice(todayContent.practice.id),
            statusMessage = "Saved locally",
            errorMessage = warningMessage
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

private fun loadPracticeContext(clock: AppClock): PracticeContext {
    val practices = ResourcePracticeRepository().loadPracticeWeeks()
    val todaySelection = TodayPracticeService(
        clock = clock,
        sequenceStartDate = SEQUENCE_START_DATE
    ).selectTodayPractice(practices)

    return PracticeContext(
        practiceWeeks = practices,
        today = TodayContent(
            practice = todaySelection.practice,
            dayIndex = todaySelection.dayIndex,
            dailyPractice = todaySelection.dailyPractice
        )
    )
}

private data class AppUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val practiceWeeks: List<PracticeWeek> = emptyList(),
    val content: TodayContent? = null,
    val reflections: List<ReflectionEntry> = emptyList(),
    val progress: PracticeProgress = PracticeProgress(),
    val reflectionText: String = "",
    val completed: Boolean = false,
    val completedCount: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

private data class PracticeContext(
    val practiceWeeks: List<PracticeWeek>,
    val today: TodayContent
)

private data class TodayContent(
    val practice: PracticeWeek,
    val dayIndex: Int,
    val dailyPractice: DailyPractice
)

private enum class TopLevelDestination(val label: String) {
    TODAY("Today"),
    WEEK("Week"),
    HISTORY("History"),
    SETTINGS("Settings")
}
