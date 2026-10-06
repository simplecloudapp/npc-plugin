package app.simplecloud.npc.common.manager

import app.simplecloud.npc.common.NoPlayers
import app.simplecloud.npc.common.RecordingHolograms
import app.simplecloud.npc.common.RecordingNpcRenderer
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.render.RoutingNpcRenderer
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryRepository
import app.simplecloud.npc.core.render.Providers
import app.simplecloud.npc.core.repository.NpcRepository
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NpcManagerReloadTest {

    private val directory = createTempDirectory("npcs")
    private val standalone = RecordingNpcRenderer()
    private val citizens = RecordingNpcRenderer()
    private val holograms = RecordingHolograms()
    private val routing = RoutingNpcRenderer(
        linkedMapOf(Providers.STANDALONE to standalone, Providers.CITIZENS to citizens),
    )
    private val context = NpcPluginContext(
        renderer = routing,
        hologramRenderer = holograms,
        playerDirectory = NoPlayers,
        inventoryRepository = InventoryRepository(createTempDirectory("inventories")),
        npcRepository = NpcRepository(directory),
        providers = routing,
    )

    private fun yaml(id: String, target: String, provider: String = Providers.STANDALONE) = """
        version: ${NpcConfig.CURRENT_VERSION}
        id: $id
        entity:
          provider: $provider
        target-servers:
          - $target
    """.trimIndent()

    @Test
    fun `reload spawns new files, despawns removed ones and refreshes edited ones`() {
        val repository = context.npcRepository
        repository.save(NpcConfig(id = "keep", targetServers = mutableListOf("Keep")))
        repository.save(NpcConfig(id = "edit", targetServers = mutableListOf("Old")))
        repository.save(NpcConfig(id = "gone", targetServers = mutableListOf("Gone")))
        repository.save(
            NpcConfig(
                id = "moved",
                entity = NpcConfig.NpcEntityConfiguration(provider = Providers.STANDALONE),
                targetServers = mutableListOf("Moved"),
            ),
        )

        directory.resolve("edit.yml").writeText(yaml("edit", "New"))
        directory.resolve("gone.yml").deleteIfExists()
        directory.resolve("fresh.yml").writeText(yaml("fresh", "Fresh"))
        directory.resolve("moved.yml").writeText(yaml("moved", "Moved", provider = Providers.CITIZENS))

        val diff = context.npcManager.reload()

        assertEquals(listOf("fresh"), diff.added.map { it.id })
        assertEquals(listOf("gone"), diff.removed.map { it.id })
        assertEquals(setOf("edit", "moved"), diff.changed.map { it.current.id }.toSet())

        assertEquals(listOf("fresh"), standalone.spawned)
        assertEquals(listOf("moved"), citizens.spawned, "a provider switch spawns at the new provider")
        assertEquals(setOf("gone", "moved"), standalone.despawned.toSet())
        assertEquals(listOf("edit"), standalone.refreshed)
        assertEquals(setOf("fresh", "edit", "moved"), holograms.updated.toSet())
        assertEquals(setOf("gone", "moved"), holograms.destroyed.toSet())
        assertNull(repository.find("gone"))
        assertEquals(listOf("New"), repository.find("edit")!!.targetServers.toList())
    }
}
