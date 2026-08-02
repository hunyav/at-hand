package com.athand.data.reflection

import com.athand.data.persistence.preserveCorruptFile
import com.athand.data.persistence.readTextIfExists
import com.athand.data.persistence.writeTextAtomically
import com.athand.domain.model.PracticeDayKey
import com.athand.domain.model.ReflectionEntry
import com.athand.domain.repository.ReflectionRepository
import com.athand.domain.repository.RepositoryLoadResult
import com.athand.platform.AppDirectories
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

private const val REFLECTIONS_SCHEMA_VERSION = 1

class JsonReflectionRepository(
    appDirectories: AppDirectories,
    private val fileName: String = "reflections.json",
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = false },
    private val nowProvider: () -> Instant = Instant::now
) : ReflectionRepository {

    private val filePath = appDirectories.appDataDirectory().resolve(fileName)

    override suspend fun loadEntries(): RepositoryLoadResult<List<ReflectionEntry>> {
        val (document, warning) = readDocumentOrDefault()
        return RepositoryLoadResult(
            data = document.entries.map { it.toDomain() },
            warning = warning
        )
    }

    override suspend fun upsert(entry: ReflectionEntry) {
        val (document, _) = readDocumentOrDefault()
        val updatedEntries = document.entries.toMutableList()
        val index = updatedEntries.indexOfFirst {
            it.practiceId == entry.dayKey.practiceId && it.dayIndex == entry.dayKey.dayIndex
        }

        val dto = ReflectionEntryDto.fromDomain(entry)
        if (index >= 0) {
            updatedEntries[index] = dto
        } else {
            updatedEntries.add(dto)
        }

        writeDocument(
            ReflectionsDocument(
                schemaVersion = REFLECTIONS_SCHEMA_VERSION,
                entries = updatedEntries
            )
        )
    }

    private suspend fun readDocumentOrDefault(): Pair<ReflectionsDocument, String?> {
        val raw = readTextIfExists(filePath) ?: return ReflectionsDocument.default() to null

        return try {
            val parsed = json.decodeFromString<ReflectionsDocument>(raw)
            validate(parsed)
            parsed to null
        } catch (error: Exception) {
            val backupPath = preserveCorruptFile(filePath, nowProvider())
            val warning = buildString {
                append("Reflections data was malformed and has been reset.")
                if (backupPath != null) {
                    append(" Backup saved to '")
                    append(backupPath)
                    append("'.")
                }
                append(" Cause: ")
                append(error.message ?: error::class.simpleName)
            }
            ReflectionsDocument.default() to warning
        }
    }

    private suspend fun writeDocument(document: ReflectionsDocument) {
        validate(document)
        val encoded = json.encodeToString(ReflectionsDocument.serializer(), document)
        writeTextAtomically(filePath, encoded)
    }

    private fun validate(document: ReflectionsDocument) {
        require(document.schemaVersion == REFLECTIONS_SCHEMA_VERSION) {
            "Unsupported reflections schema version ${document.schemaVersion}; expected $REFLECTIONS_SCHEMA_VERSION"
        }

        document.entries.forEach { entry ->
            require(entry.practiceId.isNotBlank()) { "Reflection practiceId must not be blank" }
            require(entry.dayIndex in 0..6) { "Reflection dayIndex must be between 0 and 6" }
            LocalDate.parse(entry.date)
        }
    }
}

@Serializable
private data class ReflectionsDocument(
    val schemaVersion: Int,
    val entries: List<ReflectionEntryDto>
) {
    companion object {
        fun default(): ReflectionsDocument = ReflectionsDocument(
            schemaVersion = REFLECTIONS_SCHEMA_VERSION,
            entries = emptyList()
        )
    }
}

@Serializable
private data class ReflectionEntryDto(
    val practiceId: String,
    val dayIndex: Int,
    val date: String,
    val text: String,
    val updatedAtEpochMillis: Long
) {
    fun toDomain(): ReflectionEntry = ReflectionEntry(
        dayKey = PracticeDayKey(practiceId = practiceId, dayIndex = dayIndex),
        date = LocalDate.parse(date),
        text = text,
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
    )

    companion object {
        fun fromDomain(entry: ReflectionEntry): ReflectionEntryDto = ReflectionEntryDto(
            practiceId = entry.dayKey.practiceId,
            dayIndex = entry.dayKey.dayIndex,
            date = entry.date.toString(),
            text = entry.text,
            updatedAtEpochMillis = entry.updatedAt.toEpochMilli()
        )
    }
}
