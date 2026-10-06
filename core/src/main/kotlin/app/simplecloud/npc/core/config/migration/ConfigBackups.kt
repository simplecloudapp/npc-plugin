package app.simplecloud.npc.core.config.migration

import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

object ConfigBackups {
    fun backupOutdated(directory: Path, currentVersion: Int, fallbackVersion: Int) {
        if (!directory.exists()) return
        val backups = directory.resolve("backups")
        directory.listDirectoryEntries()
            .filter { it.isRegularFile() && it.isYaml() }
            .forEach { path ->
                val version = runCatching {
                    YamlConfigurationLoader.builder().path(path).build().load().node("version").getInt(fallbackVersion)
                }.getOrDefault(fallbackVersion)
                if (version >= currentVersion) return@forEach

                val backup = backups.resolve("${path.fileName}.v$version.bak")
                if (backup.exists()) return@forEach
                backups.createDirectories()
                path.copyTo(backup)
            }
    }

    private fun Path.isYaml(): Boolean = name.lowercase().endsWith(".yml")
}
