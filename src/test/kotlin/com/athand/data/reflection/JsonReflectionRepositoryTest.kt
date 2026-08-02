package com.athand.data.reflection

import com.athand.domain.model.PracticeDayKey
import com.athand.domain.model.ReflectionEntry
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
import java.time.LocalDate

class JsonReflectionRepositoryTest {

    @Test
    fun `returns defaults when file is missing`() = runTest {
        val tempDirectory = createTempDirectory("reflections-missing")
        val repository = JsonReflectionRepository(
            appDirectories = FixedDirectories(tempDirectory)
        )

        val loadResult = repository.loadEntries()

        assertTrue(loadResult.data.isEmpty())
        assertNull(loadResult.warning)
    }

    @Test
    fun `saves and loads reflection entry`() = runTest {
        val tempDirectory = createTempDirectory("reflections-roundtrip")
        val repository = JsonReflectionRepository(
            appDirectories = FixedDirectories(tempDirectory)
        )

        val entry = ReflectionEntry(
            dayKey = PracticeDayKey(practiceId = "control-001", dayIndex = 2),
            date = LocalDate.of(2026, 8, 2),
            text = "Noticed the reaction and paused before replying.",
            updatedAt = Instant.parse("2026-08-02T12:00:00Z")
        )

        repository.upsert(entry)
        val loaded = repository.loadEntries().data

        assertEquals(1, loaded.size)
        assertEquals(entry, loaded.single())
    }

    @Test
    fun `malformed file is backed up and defaults are returned`() = runTest {
        val tempDirectory = createTempDirectory("reflections-corrupt")
        val dataDirectory = tempDirectory.resolve("data").createDirectories()
        dataDirectory.resolve("reflections.json").writeText("{ broken json")

        val repository = JsonReflectionRepository(
            appDirectories = FixedDirectories(tempDirectory),
            nowProvider = { Instant.parse("2026-08-02T15:00:00Z") }
        )

        val loadResult = repository.loadEntries()

        assertTrue(loadResult.data.isEmpty())
        assertNotNull(loadResult.warning)
        assertTrue(loadResult.warning.contains("Reflections data was malformed"))
        val backup = dataDirectory.resolve("reflections.json.corrupt-1785682800000")
        assertTrue(backup.exists())
        assertTrue(!dataDirectory.resolve("reflections.json").exists())
    }

    private class FixedDirectories(private val root: Path) : AppDirectories {
        override fun appDataDirectory(): Path = root.resolve("data")
    }
}
