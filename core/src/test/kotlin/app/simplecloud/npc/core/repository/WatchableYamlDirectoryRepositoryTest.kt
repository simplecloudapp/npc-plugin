package app.simplecloud.npc.core.repository

import app.simplecloud.npc.core.config.NpcConfig
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WatchableYamlDirectoryRepositoryTest {

    private val directory: Path = createTempDirectory("npc-watch")
    private val repository = NpcRepository(directory)
    private val events = CopyOnWriteArrayList<String>()

    init {
        repository.setExternalChangeListener { diff ->
            diff.added.forEach { events += "added:${it.id}" }
            diff.changed.forEach { change ->
                val previous = change.previous.targetServers.single()
                val current = change.current.targetServers.single()
                events += "changed:$previous->$current"
            }
            diff.removed.forEach { events += "removed:${it.id}" }
        }
    }

    private fun yaml(id: String, target: String) = """
        version: ${NpcConfig.CURRENT_VERSION}
        id: $id
        target-servers:
          - $target
    """.trimIndent()

    private fun await(what: String, timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            assertTrue(System.currentTimeMillis() < deadline, "timed out waiting for $what; events: $events")
            Thread.sleep(25)
        }
    }

    private fun settle() = Thread.sleep(700)

    @Test
    fun `an external edit reloads the config and reports an update`() {
        directory.resolve("lobby.yml").writeText(yaml("lobby", "Lobby"))
        repository.loadAndWatch()
        try {
            assertEquals(listOf("Lobby"), repository.find("lobby")!!.targetServers.toList())

            settle()
            directory.resolve("lobby.yml").writeText(yaml("lobby", "Hub"))

            await("change event") { events.contains("changed:Lobby->Hub") }
            await("reloaded target") { repository.find("lobby")?.targetServers == listOf("Hub") }
        } finally {
            repository.stopWatching()
        }
    }

    @Test
    fun `a new file appears and an external delete reports a deletion`() {
        repository.loadAndWatch()
        try {
            settle()
            directory.resolve("shop.yml").writeText(yaml("shop", "Shop"))
            await("created config") { repository.find("shop") != null }
            await("added event for the new file") { events.contains("added:shop") }

            directory.resolve("shop.yml").deleteIfExists()
            await("removed event") { events.contains("removed:shop") }
            assertNull(repository.find("shop"))
        } finally {
            repository.stopWatching()
        }
    }

    @Test
    fun `the repository's own save does not echo back as an external update`() {
        repository.loadAndWatch()
        try {
            settle()
            repository.save(NpcConfig(id = "own", targetServers = mutableListOf("Own")))
            assertTrue(directory.resolve("own.yml").exists())

            settle()
            assertTrue(events.isEmpty(), "own writes must not reload: $events")
            assertNotNull(repository.find("own"))
        } finally {
            repository.stopWatching()
        }
    }

    @Test
    fun `a deleted and recreated directory is watched again`() {
        directory.resolve("lobby.yml").writeText(yaml("lobby", "Lobby"))
        repository.loadAndWatch()
        try {
            settle()
            directory.toFile().deleteRecursively()
            await("removed event") { events.contains("removed:lobby") }

            directory.toFile().mkdirs()
            directory.resolve("lobby.yml").writeText(yaml("lobby", "Hub"))
            await("reloaded after the directory came back") { repository.find("lobby")?.targetServers == listOf("Hub") }

            settle()
            directory.resolve("lobby.yml").writeText(yaml("lobby", "Shop"))
            await("edit after rewatch") { events.contains("changed:Hub->Shop") }
        } finally {
            repository.stopWatching()
        }
    }
}
