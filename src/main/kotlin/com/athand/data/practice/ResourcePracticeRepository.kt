package com.athand.data.practice

import com.athand.domain.model.PracticeWeek
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val BUNDLED_PRACTICES_SCHEMA_VERSION = 1

class ResourcePracticeRepository(
    private val resourcePath: String = "practices/practice-weeks.json",
    private val json: Json = Json { ignoreUnknownKeys = false },
    private val readResource: (String) -> String? = ::readResourceFromClasspath
) {
    fun loadPracticeWeeks(): List<PracticeWeek> {
        val content = readResource(resourcePath)
            ?: throw IllegalStateException("Bundled practices resource not found at '$resourcePath'")

        val document = json.decodeFromString<PracticeWeeksDocument>(content)
        validate(document)
        return document.weeks
    }

    private fun validate(document: PracticeWeeksDocument) {
        require(document.schemaVersion == BUNDLED_PRACTICES_SCHEMA_VERSION) {
            "Unsupported practice schema version ${document.schemaVersion}; expected $BUNDLED_PRACTICES_SCHEMA_VERSION"
        }
        require(document.weeks.isNotEmpty()) { "Bundled practice list is empty" }

        val duplicateIds = document.weeks
            .groupingBy { it.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateIds.isEmpty()) { "Duplicate practice ids: ${duplicateIds.joinToString()}" }

        document.weeks.forEach { week ->
            require(week.id.isNotBlank()) { "Practice week id must not be blank" }
            require(week.title.isNotBlank()) { "Practice week '${week.id}' title must not be blank" }
            require(week.principle.isNotBlank()) { "Practice week '${week.id}' principle must not be blank" }
            require(week.overview.isNotBlank()) { "Practice week '${week.id}' overview must not be blank" }
            require(week.dailyPractices.size == 7) {
                "Practice week '${week.id}' must contain exactly 7 daily practices"
            }

            val dayIndices = week.dailyPractices.map { it.dayIndex }
            require(dayIndices.toSet().size == 7) {
                "Practice week '${week.id}' must contain unique day indexes 0..6"
            }
            require(dayIndices.toSet() == (0..6).toSet()) {
                "Practice week '${week.id}' day indexes must be exactly 0..6"
            }

            week.dailyPractices.forEach { day ->
                require(day.heading.isNotBlank()) {
                    "Practice week '${week.id}' day ${day.dayIndex} heading must not be blank"
                }
                require(day.instruction.isNotBlank()) {
                    "Practice week '${week.id}' day ${day.dayIndex} instruction must not be blank"
                }
            }
        }
    }
}

private fun readResourceFromClasspath(path: String): String? {
    return ResourcePracticeRepository::class.java.classLoader
        .getResource(path)
        ?.readText()
}

@Serializable
private data class PracticeWeeksDocument(
    val schemaVersion: Int,
    val weeks: List<PracticeWeek>
)
