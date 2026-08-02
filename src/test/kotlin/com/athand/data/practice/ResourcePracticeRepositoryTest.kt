package com.athand.data.practice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ResourcePracticeRepositoryTest {

    @Test
    fun `loads bundled practice weeks from resources`() {
        val repository = ResourcePracticeRepository()

        val weeks = repository.loadPracticeWeeks()

        assertEquals(3, weeks.size)
        assertEquals("control-001", weeks.first().id)
        assertEquals(7, weeks.first().dailyPractices.size)
    }

    @Test
    fun `fails when schema version is unsupported`() {
        val repository = ResourcePracticeRepository(
            readResource = {
                """
                {
                  "schemaVersion": 2,
                  "weeks": []
                }
                """.trimIndent()
            }
        )

        assertFailsWith<IllegalArgumentException> {
            repository.loadPracticeWeeks()
        }
    }

    @Test
    fun `fails when week does not contain all day indexes`() {
        val repository = ResourcePracticeRepository(
            readResource = {
                """
                {
                  "schemaVersion": 1,
                  "weeks": [
                    {
                      "id": "broken",
                      "title": "Broken",
                      "principle": "Broken principle",
                      "overview": "Broken overview",
                      "dailyPractices": [
                        {"dayIndex": 0, "heading": "h0", "instruction": "i0"},
                        {"dayIndex": 1, "heading": "h1", "instruction": "i1"},
                        {"dayIndex": 2, "heading": "h2", "instruction": "i2"},
                        {"dayIndex": 3, "heading": "h3", "instruction": "i3"},
                        {"dayIndex": 4, "heading": "h4", "instruction": "i4"},
                        {"dayIndex": 5, "heading": "h5", "instruction": "i5"},
                        {"dayIndex": 5, "heading": "h5b", "instruction": "i5b"}
                      ]
                    }
                  ]
                }
                """.trimIndent()
            }
        )

        assertFailsWith<IllegalArgumentException> {
            repository.loadPracticeWeeks()
        }
    }
}
