package com.athand.data.progress

import com.athand.domain.model.PracticeDayKey
import com.athand.platform.AppDirectories
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.nio.file.Path
import java.time.Instant

class JsonProgressRepositoryTest {

    @Test
    fun `returns empty progress when file is missing`() = runTest {
        val tempDirectory = createTempDirectory("progress-missing")
        val repository = JsonProgressRepository(
            appDirectories = FixedDirectories(tempDirectory)
        )

        val loadResult = repository.loadProgress()

        assertEquals(0, loadResult.data.completedDays.size)
        assertNull(loadResult.warning)
    }

    @Test
    fun `saves completion and loads it back`() = runTest {
        val tempDirectory = createTempDirectory("progress-roundtrip")
        val repository = JsonProgressRepository(
            appDirectories = FixedDirectories(tempDirectory)
        )

        val dayKey = PracticeDayKey(practiceId = "values-003", dayIndex = 4)
        repository.setCompleted(
            dayKey = dayKey,
            completed = true,
            completedAt = Instant.parse("2026-08-02T13:00:00Z")
        )

        val loaded = repository.loadProgress().data
        assertTrue(loaded.isCompleted(dayKey))
        assertEquals(1, loaded.completedCountForPractice("values-003"))
    }

    @Test
    fun `can clear completion state`() = runTest {
        val tempDirectory = createTempDirectory("progress-clear")
        val repository = JsonProgressRepository(
            appDirectories = FixedDirectories(tempDirectory)
        )

        val dayKey = PracticeDayKey(practiceId = "judgment-002", dayIndex = 1)
        val timestamp = Instant.parse("2026-08-02T13:00:00Z")
        repository.setCompleted(dayKey = dayKey, completed = true, completedAt = timestamp)
        repository.setCompleted(dayKey = dayKey, completed = false, completedAt = timestamp)

        val loaded = repository.loadProgress().data
        assertTrue(!loaded.isCompleted(dayKey))
        assertEquals(0, loaded.completedCountForPractice("judgment-002"))
    }

    @Test
    fun `malformed file is backed up and defaults are returned`() = runTest {
        val tempDirectory = createTempDirectory("progress-corrupt")
        val dataDirectory = tempDirectory.resolve("data").createDirectories()
        dataDirectory.resolve("progress.json").writeText("{ malformed")

        val repository = JsonProgressRepository(
            appDirectories = FixedDirectories(tempDirectory),
            nowProvider = { Instant.parse("2026-08-02T15:30:00Z") }
        )

        val loadResult = repository.loadProgress()

        assertEquals(0, loadResult.data.completedDays.size)
        assertNotNull(loadResult.warning)
        assertTrue(loadResult.warning.contains("Progress data was malformed"))
        val backup = dataDirectory.resolve("progress.json.corrupt-1785684600000")
        assertTrue(backup.exists())
        assertTrue(!dataDirectory.resolve("progress.json").exists())
    }

    private class FixedDirectories(private val root: Path) : AppDirectories {
        override fun appDataDirectory(): Path = root.resolve("data")
    }
}
