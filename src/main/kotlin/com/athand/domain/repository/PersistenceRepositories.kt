package com.athand.domain.repository

import com.athand.domain.model.PracticeDayKey
import com.athand.domain.model.PracticeProgress
import com.athand.domain.model.ReflectionEntry
import java.time.Instant

data class RepositoryLoadResult<T>(
    val data: T,
    val warning: String? = null
)

interface ReflectionRepository {
    suspend fun loadEntries(): RepositoryLoadResult<List<ReflectionEntry>>

    suspend fun upsert(entry: ReflectionEntry)
}

interface ProgressRepository {
    suspend fun loadProgress(): RepositoryLoadResult<PracticeProgress>

    suspend fun setCompleted(dayKey: PracticeDayKey, completed: Boolean, completedAt: Instant)
}
