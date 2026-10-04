package app.simplecloud.npc.common.manager

import app.simplecloud.npc.common.NoHolograms
import app.simplecloud.npc.common.NoPlayers
import app.simplecloud.npc.common.RecordingNpcRenderer
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.render.RoutingNpcRenderer
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryRepository
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.core.render.NpcRenderer
import app.simplecloud.npc.core.render.ProviderNpcSnapshot
import app.simplecloud.npc.core.repository.NpcRepository
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NpcManagerUpgradeTest {

    private class FakeProvider(private val persisted: MutableMap<String, ProviderNpcSnapshot>) : NpcRenderer {
        val spawned = mutableListOf<NpcConfig>()
        val deletedReferences = mutableListOf<String>()

        override fun onEnable() = Unit
        override fun onDisable() = Unit
        override fun spawn(config: NpcConfig): NpcConfig = config.also(spawned::add)
        override fun despawn(config: NpcConfig) {
            if (config.entity.providerLinked) return
            config.entity.providerReference?.let { if (persisted.remove(it) != null) deletedReferences += it }
        }

        override fun refresh(config: NpcConfig) = Unit
        override fun teleport(config: NpcConfig) = Unit
        override fun locationOf(config: NpcConfig): NpcLocation? = null
        override fun snapshotOf(config: NpcConfig): ProviderNpcSnapshot? =
            config.entity.providerReference?.let(persisted::get)
    }

    private fun context(citizens: FakeProvider): NpcPluginContext {
        val routing = RoutingNpcRenderer(linkedMapOf("citizens" to citizens, "standalone" to RecordingNpcRenderer()))
        return NpcPluginContext(
            renderer = routing,
            hologramRenderer = NoHolograms,
            playerDirectory = NoPlayers,
            inventoryRepository = InventoryRepository(createTempDirectory("inventories")),
            npcRepository = NpcRepository(createTempDirectory("npcs")),
            providers = routing,
        )
    }

    private val lobbyLocation = NpcLocation("lobby", 10.5, 64.0, -3.5, 90f, 0f)
    private val skin = NpcConfig.SkinConfiguration("texture", "signature", "Notch")

    @Test
    fun `a version five npc keeps its citizens location, skin and name`() {
        val citizens = FakeProvider(mutableMapOf("42" to ProviderNpcSnapshot(lobbyLocation, skin, "Lobby Guide")))
        val manager = context(citizens).npcManager

        val migrated = NpcConfig(
            id = "lobby",
            entity = NpcConfig.NpcEntityConfiguration(
                provider = "citizens",
                providerReference = "42",
                needsRelocation = true,
            ),
        )
        val reconciled = manager.reconcileProvider(migrated)

        assertEquals(lobbyLocation, reconciled.entity.location)
        assertEquals(skin, reconciled.entity.skin)
        assertEquals("Lobby Guide", reconciled.entity.customName)
        assertFalse(reconciled.entity.needsRelocation)
        assertEquals("standalone", reconciled.entity.provider)
        assertNull(reconciled.entity.providerReference)
        assertEquals(listOf("42"), citizens.deletedReferences)
    }

    @Test
    fun `an npc whose provider npc is gone keeps the relocation warning`() {
        val citizens = FakeProvider(mutableMapOf())
        val manager = context(citizens).npcManager

        val migrated = NpcConfig(
            id = "lobby",
            entity = NpcConfig.NpcEntityConfiguration(
                provider = "citizens",
                providerReference = "42",
                needsRelocation = true,
            ),
        )
        val reconciled = manager.reconcileProvider(migrated)

        assertTrue(reconciled.entity.needsRelocation)
        assertEquals(NpcLocation(), reconciled.entity.location)
        assertTrue(citizens.deletedReferences.isEmpty())
    }

    @Test
    fun `a linked npc is never adopted or replaced`() {
        val citizens = FakeProvider(mutableMapOf("42" to ProviderNpcSnapshot(lobbyLocation, skin, "Shopkeeper")))
        val manager = context(citizens).npcManager

        val linked = NpcConfig(
            id = "shop",
            entity = NpcConfig.NpcEntityConfiguration(
                provider = "citizens",
                providerReference = "42",
                providerLinked = true,
            ),
        )

        assertEquals(linked, manager.reconcileProvider(linked))
        assertTrue(citizens.deletedReferences.isEmpty())
    }

    @Test
    fun `a managed npc of another plugin becomes an own npc in place`() {
        val citizens = FakeProvider(mutableMapOf("42" to ProviderNpcSnapshot(lobbyLocation, skin)))
        val manager = context(citizens).npcManager
        val hub = NpcLocation("hub", 1.0, 2.0, 3.0)

        val managed = NpcConfig(
            id = "lobby",
            entity = NpcConfig.NpcEntityConfiguration(provider = "citizens", providerReference = "42", location = hub),
        )
        val reconciled = manager.reconcileProvider(managed)

        assertEquals("standalone", reconciled.entity.provider)
        assertEquals(hub, reconciled.entity.location)
        assertNull(reconciled.entity.providerReference)
        assertEquals(listOf("42"), citizens.deletedReferences)
    }
}
