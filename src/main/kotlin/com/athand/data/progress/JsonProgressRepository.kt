package com.athand.data.progress

import com.athand.data.persistence.preserveCorruptFile
import com.athand.data.persistence.readTextIfExists
import com.athand.data.persistence.writeTextAtomically
import com.athand.domain.model.PracticeCompletion
import com.athand.domain.model.PracticeDayKey
import com.athand.domain.model.PracticeProgress
import com.athand.domain.repository.ProgressRepository
import com.athand.domain.repository.RepositoryLoadResult
import com.athand.platform.AppDirectories
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

private const val PROGRESS_SCHEMA_VERSION = 1

class JsonProgressRepository(
    appDirectories: AppDirectories,
    private val fileName: String = "progress.json",
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = false },
    private val nowProvider: () -> Instant = Instant::now
) : ProgressRepository {

    private val filePath = appDirectories.appDataDirectory().resolve(fileName)

    override suspend fun loadProgress(): RepositoryLoadResult<PracticeProgress> {
        val (document, warning) = readDocumentOrDefault()
        return RepositoryLoadResult(
            data = PracticeProgress(
                completedDays = document.completedDays.map { it.toDomain() }.toSet()
            ),
            warning = warning
        )
    }

    override suspend fun setCompleted(dayKey: PracticeDayKey, completed: Boolean, completedAt: Instant) {
        val (document, _) = readDocumentOrDefault()
        val byDay = document.completedDays
            .associateBy { it.practiceId to it.dayIndex }
            .toMutableMap()

        if (completed) {
            byDay[dayKey.practiceId to dayKey.dayIndex] = CompletedDayDto(
                practiceId = dayKey.practiceId,
                dayIndex = dayKey.dayIndex,
                completedAtEpochMillis = completedAt.toEpochMilli()
            )
        } else {
            byDay.remove(dayKey.practiceId to dayKey.dayIndex)
        }

        writeDocument(
            ProgressDocument(
                schemaVersion = PROGRESS_SCHEMA_VERSION,
                completedDays = byDay.values.toList()
            )
        )
    }

    private suspend fun readDocumentOrDefault(): Pair<ProgressDocument, String?> {
        val raw = readTextIfExists(filePath) ?: return ProgressDocument.default() to null

        return try {
            val parsed = json.decodeFromString<ProgressDocument>(raw)
            validate(parsed)
            parsed to null
        } catch (error: Exception) {
            val backupPath = preserveCorruptFile(filePath, nowProvider())
            val warning = buildString {
                append("Progress data was malformed and has been reset.")
                if (backupPath != null) {
                    append(" Backup saved to '")
                    append(backupPath)
                    append("'.")
                }
                append(" Cause: ")
                append(error.message ?: error::class.simpleName)
            }
            ProgressDocument.default() to warning
        }
    }

    private suspend fun writeDocument(document: ProgressDocument) {
        validate(document)
        val encoded = json.encodeToString(ProgressDocument.serializer(), document)
        writeTextAtomically(filePath, encoded)
    }

    private fun validate(document: ProgressDocument) {
        require(document.schemaVersion == PROGRESS_SCHEMA_VERSION) {
            "Unsupported progress schema version ${document.schemaVersion}; expected $PROGRESS_SCHEMA_VERSION"
        }

        val duplicateKeys = document.completedDays
            .groupingBy { it.practiceId to it.dayIndex }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        require(duplicateKeys.isEmpty()) { "Progress contains duplicate day completion entries" }

        document.completedDays.forEach { entry ->
            require(entry.practiceId.isNotBlank()) { "Progress practiceId must not be blank" }
            require(entry.dayIndex in 0..6) { "Progress dayIndex must be between 0 and 6" }
        }
    }
}

@Serializable
private data class ProgressDocument(
    val schemaVersion: Int,
    val completedDays: List<CompletedDayDto>
) {
    companion object {
        fun default(): ProgressDocument = ProgressDocument(
            schemaVersion = PROGRESS_SCHEMA_VERSION,
            completedDays = emptyList()
        )
    }
}

@Serializable
private data class CompletedDayDto(
    val practiceId: String,
    val dayIndex: Int,
    val completedAtEpochMillis: Long
) {
    fun toDomain(): PracticeCompletion = PracticeCompletion(
        dayKey = PracticeDayKey(practiceId = practiceId, dayIndex = dayIndex),
        completedAt = Instant.ofEpochMilli(completedAtEpochMillis)
    )
}
