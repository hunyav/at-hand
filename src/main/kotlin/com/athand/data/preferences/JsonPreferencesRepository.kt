package com.athand.data.preferences

import com.athand.data.persistence.preserveCorruptFile
import com.athand.data.persistence.readTextIfExists
import com.athand.data.persistence.writeTextAtomically
import com.athand.domain.model.UserPreferences
import com.athand.domain.repository.PreferencesRepository
import com.athand.domain.repository.RepositoryLoadResult
import com.athand.platform.AppDirectories
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalTime

private const val PREFERENCES_SCHEMA_VERSION = 1

class JsonPreferencesRepository(
    appDirectories: AppDirectories,
    private val fileName: String = "preferences.json",
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = false },
    private val nowProvider: () -> Instant = Instant::now
) : PreferencesRepository {

    private val filePath = appDirectories.appDataDirectory().resolve(fileName)

    override suspend fun loadPreferences(): RepositoryLoadResult<UserPreferences> {
        val (document, warning) = readDocumentOrDefault()
        return RepositoryLoadResult(
            data = document.toDomain(),
            warning = warning
        )
    }

    override suspend fun savePreferences(preferences: UserPreferences) {
        val document = PreferencesDocument.fromDomain(preferences)
        writeDocument(document)
    }

    private suspend fun readDocumentOrDefault(): Pair<PreferencesDocument, String?> {
        val raw = readTextIfExists(filePath) ?: return PreferencesDocument.default() to null

        return try {
            val parsed = json.decodeFromString<PreferencesDocument>(raw)
            validate(parsed)
            parsed to null
        } catch (error: Exception) {
            val backupPath = preserveCorruptFile(filePath, nowProvider())
            val warning = buildString {
                append("Preferences data was malformed and has been reset.")
                if (backupPath != null) {
                    append(" Backup saved to '")
                    append(backupPath)
                    append("'.")
                }
                append(" Cause: ")
                append(error.message ?: error::class.simpleName)
            }
            PreferencesDocument.default() to warning
        }
    }

    private suspend fun writeDocument(document: PreferencesDocument) {
        validate(document)
        val encoded = json.encodeToString(PreferencesDocument.serializer(), document)
        writeTextAtomically(filePath, encoded)
    }

    private fun validate(document: PreferencesDocument) {
        require(document.schemaVersion == PREFERENCES_SCHEMA_VERSION) {
            "Unsupported preferences schema version ${document.schemaVersion}; expected $PREFERENCES_SCHEMA_VERSION"
        }

        if (document.reminderTime != null) {
            LocalTime.parse(document.reminderTime)
        }
    }
}

@Serializable
private data class PreferencesDocument(
    val schemaVersion: Int,
    val reminderEnabled: Boolean,
    val reminderTime: String?
) {
    fun toDomain(): UserPreferences = UserPreferences(
        reminderEnabled = reminderEnabled,
        reminderTime = reminderTime?.let(LocalTime::parse)
    )

    companion object {
        fun default(): PreferencesDocument = PreferencesDocument(
            schemaVersion = PREFERENCES_SCHEMA_VERSION,
            reminderEnabled = false,
            reminderTime = null
        )

        fun fromDomain(preferences: UserPreferences): PreferencesDocument = PreferencesDocument(
            schemaVersion = PREFERENCES_SCHEMA_VERSION,
            reminderEnabled = preferences.reminderEnabled,
            reminderTime = preferences.reminderTime?.toString()
        )
    }
}
