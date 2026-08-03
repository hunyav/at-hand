package com.athand.data.preferences

import com.athand.domain.model.UserPreferences
import com.athand.platform.AppDirectories
import kotlinx.coroutines.test.runTest
import java.nio.file.Path
import java.time.Instant
import java.time.LocalTime
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JsonPreferencesRepositoryTest {

    @Test
    fun `returns default preferences when file is missing`() = runTest {
        val tempDirectory = createTempDirectory("preferences-missing")
        val repository = JsonPreferencesRepository(
            appDirectories = FixedDirectories(tempDirectory)
        )

        val loadResult = repository.loadPreferences()

        assertEquals(UserPreferences(), loadResult.data)
        assertNull(loadResult.warning)
    }

    @Test
    fun `saves and loads preferences`() = runTest {
        val tempDirectory = createTempDirectory("preferences-roundtrip")
        val repository = JsonPreferencesRepository(
            appDirectories = FixedDirectories(tempDirectory)
        )

        val preferences = UserPreferences(
            reminderEnabled = true,
            reminderTime = LocalTime.of(9, 30)
        )

        repository.savePreferences(preferences)
        val loadResult = repository.loadPreferences()

        assertEquals(preferences, loadResult.data)
        assertNull(loadResult.warning)
    }

    @Test
    fun `malformed file is backed up and defaults are returned`() = runTest {
        val tempDirectory = createTempDirectory("preferences-corrupt")
        val dataDirectory = tempDirectory.resolve("data").createDirectories()
        dataDirectory.resolve("preferences.json").writeText("{ broken json")

        val repository = JsonPreferencesRepository(
            appDirectories = FixedDirectories(tempDirectory),
            nowProvider = { Instant.parse("2026-08-02T15:00:00Z") }
        )

        val loadResult = repository.loadPreferences()

        assertEquals(UserPreferences(), loadResult.data)
        assertNotNull(loadResult.warning)
        assertTrue(loadResult.warning.contains("Preferences data was malformed"))
        val backup = dataDirectory.resolve("preferences.json.corrupt-1785682800000")
        assertTrue(backup.exists())
        assertTrue(!dataDirectory.resolve("preferences.json").exists())
    }

    private class FixedDirectories(private val root: Path) : AppDirectories {
        override fun appDataDirectory(): Path = root.resolve("data")
    }
}
