package app.simplecloud.npc.bukkit.provider

import app.simplecloud.npc.bukkit.proxy
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import org.bukkit.World
import org.bukkit.event.Listener
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinkedProviderRendererTest {

    private class FakeNpc(val key: String)

    private class FakeRenderer(
        override val supportsCreation: Boolean = true,
    ) : LinkedProviderRenderer<FakeNpc>(
        plugin = proxy(),
        providerLabel = "Fake",
        worldLookup = { name -> if (name == "world") proxy<World>() else null },
        sync = { block -> block() },
    ) {
        val registry = mutableMapOf<String, FakeNpc>()
        val created = mutableListOf<String>()
        val destroyed = mutableListOf<String>()
        val deletedLegacy = mutableListOf<String>()
        val refreshed = mutableListOf<String>()
        val teleported = mutableListOf<String>()

        override fun eventKeyOf(tracked: FakeNpc): String = "event:${tracked.key}"
        override fun findByReference(reference: String): FakeNpc? = registry[reference]
        val createdNpcs = mutableMapOf<String, FakeNpc>()

        override fun create(config: NpcConfig, world: World): FakeNpc =
            FakeNpc("created:${config.id}").also {
                created += config.id
                createdNpcs[config.id] = it
            }

        override fun destroy(tracked: FakeNpc) {
            destroyed += tracked.key
        }

        override fun deleteLegacy(legacy: FakeNpc) {
            deletedLegacy += legacy.key
        }

        override fun linkCandidates(): List<Pair<String, FakeNpc>> = registry.toList()

        override fun createListener(): Listener = object : Listener {}
        override fun refreshOwned(config: NpcConfig, tracked: FakeNpc) {
            refreshed += tracked.key
        }

        override fun teleportOwned(config: NpcConfig, tracked: FakeNpc, world: World) {
            teleported += tracked.key
        }

        override fun locationOf(tracked: FakeNpc): NpcLocation = NpcLocation("world", 1.0, 2.0, 3.0)

        fun isTracked(configId: String): Boolean = tracked(configId) != null
        fun linked(configId: String): Boolean = isLinked(configId)
    }

    private fun config(id: String, reference: String? = null, linked: Boolean = false) =
        NpcConfig(
            id = id,
            entity = NpcConfig.NpcEntityConfiguration(
                location = NpcLocation("world"),
                providerReference = reference,
                providerLinked = linked,
            ),
        )

    @Test
    fun `an owned npc is created on spawn and destroyed on despawn`() {
        val renderer = FakeRenderer()

        renderer.spawn(config("shop"))

        assertEquals(listOf("shop"), renderer.created)
        assertTrue(renderer.isTracked("shop"))
        assertFalse(renderer.linked("shop"))
        assertEquals("shop", renderer.configIdForEventKey("event:created:shop"))

        renderer.despawn(config("shop"))

        assertEquals(listOf("created:shop"), renderer.destroyed)
        assertFalse(renderer.isTracked("shop"))
        assertNull(renderer.configIdForEventKey("event:created:shop"))
    }

    @Test
    fun `a linked npc attaches to the provider's own npc and is never destroyed`() {
        val renderer = FakeRenderer()
        renderer.registry["42"] = FakeNpc("theirs")

        renderer.spawn(config("shop", reference = "42", linked = true))

        assertTrue(renderer.created.isEmpty())
        assertTrue(renderer.linked("shop"))
        assertEquals("shop", renderer.configIdForEventKey("event:theirs"))

        renderer.despawn(config("shop", reference = "42", linked = true))

        assertTrue(renderer.destroyed.isEmpty())
        assertTrue(renderer.deletedLegacy.isEmpty())
        assertFalse(renderer.isTracked("shop"))
    }

    @Test
    fun `an npc we own for one config cannot be linked by another`() {
        val renderer = FakeRenderer()
        renderer.spawn(config("owner"))
        val owned = renderer.createdNpcs.getValue("owner")
        renderer.registry["owners-npc"] = owned
        renderer.registry["42"] = FakeNpc("theirs")

        assertEquals(listOf("42"), renderer.linkableReferences())

        renderer.spawn(config("thief", reference = "owners-npc", linked = true))

        assertFalse(renderer.isTracked("thief"))
        assertEquals("owner", renderer.configIdForEventKey("event:created:owner"))
    }
}
