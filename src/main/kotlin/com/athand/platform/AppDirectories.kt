package com.athand.platform

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.div

interface AppDirectories {
    fun appDataDirectory(): Path
}

class DefaultAppDirectories(
    private val osName: String = System.getProperty("os.name"),
    private val userHome: Path = Paths.get(System.getProperty("user.home")),
    private val appDataEnv: String? = System.getenv("APPDATA")
) : AppDirectories {

    override fun appDataDirectory(): Path {
        val normalizedOs = osName.lowercase()
        return when {
            normalizedOs.contains("win") -> {
                val base = appDataEnv?.takeIf { it.isNotBlank() }?.let(Paths::get)
                    ?: userHome / "AppData" / "Roaming"
                base / "AtHand"
            }

            normalizedOs.contains("mac") -> userHome / "Library" / "Application Support" / "At Hand"
            else -> userHome / ".local" / "share" / "at-hand"
        }
    }
}
