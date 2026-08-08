package app.simplecloud.npc.plugin.paper

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PluginDataFolderMigrationTest {

    @Test
    fun `renames the legacy data folder without losing files`() {
        val pluginsDirectory = createTempDirectory("npc-folder-migration")
        val legacy = pluginsDirectory.resolve(PluginDataFolderMigration.LEGACY_FOLDER_NAME)
        legacy.resolve("npcs").createDirectories()
        legacy.resolve("messages.yml").writeText("message: legacy")
        legacy.resolve("npcs/lobby.yml").writeText("id: lobby")
        val current = pluginsDirectory.resolve("simplecloud-npc")

        val result = PluginDataFolderMigration.migrate(current)

        assertIs<PluginDataFolderMigration.Result.Moved>(result)
        assertEquals("message: legacy", current.resolve("messages.yml").readText())
        assertEquals("id: lobby", current.resolve("npcs/lobby.yml").readText())
        assertTrue(hasExactEntry(pluginsDirectory, "simplecloud-npc"))
        assertFalse(hasExactEntry(pluginsDirectory, PluginDataFolderMigration.LEGACY_FOLDER_NAME))
    }

    @Test
    fun `merges files without replacing conflicting data`() {
        val directory = createTempDirectory("npc-folder-merge")
        val source = directory.resolve("legacy").createDirectories()
        val target = directory.resolve("current").createDirectories()
        source.resolve("npcs").createDirectories()
        target.resolve("npcs").createDirectories()
        source.resolve("npcs/legacy.yml").writeText("legacy npc")
        source.resolve("messages.yml").writeText("legacy messages")
        target.resolve("messages.yml").writeText("current messages")

        val conflicts = PluginDataFolderMigration.mergeDirectories(source, target)

        assertEquals(listOf("messages.yml"), conflicts.map { it.toString() })
        assertEquals("legacy npc", target.resolve("npcs/legacy.yml").readText())
        assertEquals("current messages", target.resolve("messages.yml").readText())
        assertEquals("legacy messages", source.resolve("messages.yml").readText())
    }

    @Test
    fun `removes duplicate legacy files after merging`() {
        val directory = createTempDirectory("npc-folder-duplicates")
        val source = directory.resolve("legacy").createDirectories()
        val target = directory.resolve("current").createDirectories()
        source.resolve("messages.yml").writeText("same")
        target.resolve("messages.yml").writeText("same")

        val conflicts = PluginDataFolderMigration.mergeDirectories(source, target)

        assertTrue(conflicts.isEmpty())
        assertFalse(Files.exists(source))
        assertEquals("same", target.resolve("messages.yml").readText())
    }

    private fun hasExactEntry(parent: java.nio.file.Path, name: String): Boolean {
        return Files.newDirectoryStream(parent).use { entries ->
            entries.any { it.fileName.toString() == name }
        }
    }
}
