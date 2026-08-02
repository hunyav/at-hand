package com.athand.data.persistence

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Instant

internal suspend fun readTextIfExists(path: Path): String? = withContext(Dispatchers.IO) {
    if (Files.exists(path)) {
        Files.readString(path)
    } else {
        null
    }
}

internal suspend fun writeTextAtomically(path: Path, content: String) = withContext(Dispatchers.IO) {
    val parent = path.parent
    if (parent != null) {
        Files.createDirectories(parent)
    }

    val tempPath = path.resolveSibling("${path.fileName}.tmp")
    Files.writeString(
        tempPath,
        content,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
    )

    try {
        try {
            Files.move(tempPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tempPath, path, StandardCopyOption.REPLACE_EXISTING)
        }
    } finally {
        Files.deleteIfExists(tempPath)
    }
}

internal suspend fun preserveCorruptFile(path: Path, now: Instant): Path? = withContext(Dispatchers.IO) {
    if (!Files.exists(path)) {
        return@withContext null
    }

    val backupPath = path.resolveSibling("${path.fileName}.corrupt-${now.toEpochMilli()}")
    Files.move(path, backupPath, StandardCopyOption.REPLACE_EXISTING)
    backupPath
}
