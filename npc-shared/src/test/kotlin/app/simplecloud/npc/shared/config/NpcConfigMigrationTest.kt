package app.simplecloud.npc.shared.config

import app.simplecloud.npc.shared.provider.NpcProviderRegistry
import app.simplecloud.npc.shared.provider.NpcProviderType
import app.simplecloud.npc.shared.provider.ProviderOwnership
import app.simplecloud.npc.shared.repository.NpcRepository
import app.simplecloud.npc.shared.utils.ConfigVersion
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.hologram.config.HologramConfiguration
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NpcConfigMigrationTest {

    @Test
    fun `uses safe pushback defaults for new npcs`() {
        val pushback = NpcConfig.PushbackConfiguration()

        assertEquals(1.2, pushback.radius)
        assertEquals(0.8, pushback.strength)
    }

    @Test
    fun `writes the provider binding beside lowercase join states`() {
        val directory = createTempDirectory("npc-config-write")
        val repository = NpcRepository(directory, NpcProviderRegistry(emptyList()))
        repository.save(
            NpcConfig(
                id = "lobby",
                provider = NpcConfig.ProviderConfiguration(
                    NpcProviderType.CITIZENS,
                    "42",
                    ProviderOwnership.MANAGED,
                ),
                targetServers = mutableListOf("Lobby"),
                hologram = NpcConfig.HologramConfigurationRoot(
                    layouts = mutableListOf(
                        NpcConfig.HologramLayout("MAINTENANCE", mutableListOf(HologramConfiguration("Hello")))
                    )
                ),
                actions = mutableListOf(
                    NpcConfig.ActionConfiguration(PlayerInteraction.RIGHT_CLICK, "DEFAULT", joinTarget = true)
                ),
            )
        )

        val yaml = directory.resolve("lobby.yml").readText()
        assertTrue(yaml.contains("provider:"))
        assertTrue(yaml.contains("reference: '42'") || yaml.contains("reference: 42"))
        assertTrue(yaml.contains("target-servers:"))
        assertTrue(yaml.contains("join-state: maintenance"))
        assertTrue(yaml.contains("join-state: default"))
    }

    @Test
    fun `resolves and suggests unambiguous provider references as npc selectors`() {
        val directory = createTempDirectory("npc-selectors")
        val repository = NpcRepository(directory, NpcProviderRegistry(emptyList()))
        repository.save(
            NpcConfig(
                id = "lobby",
                provider = NpcConfig.ProviderConfiguration(
                    NpcProviderType.ZNPCS_PLUS,
                    "server-selector",
                    ProviderOwnership.LINKED,
                ),
            )
        )

        assertEquals("lobby", repository.findBySelector("server-selector")?.id)
        assertEquals(listOf("lobby", "server-selector"), repository.selectors())
    }

    @Test
    fun `does not expose ambiguous provider references as npc selectors`() {
        val directory = createTempDirectory("ambiguous-npc-selectors")
        val repository = NpcRepository(directory, NpcProviderRegistry(emptyList()))
        repository.save(
            NpcConfig(
                id = "first",
                provider = NpcConfig.ProviderConfiguration(NpcProviderType.CITIZENS, "42"),
            )
        )
        repository.save(
            NpcConfig(
                id = "second",
                provider = NpcConfig.ProviderConfiguration(NpcProviderType.ZNPCS_PLUS, "42"),
            )
        )

        assertEquals(null, repository.findBySelector("42"))
        assertEquals(listOf("first", "second"), repository.selectors())
    }

    @Test
    fun `migrates the proposed version one structure and normalizes join states`() {
        val directory = createTempDirectory("npc-config-migration")
        directory.resolve("test.yml").writeText(
            """
            version: '1'
            id: test
            provider: CITIZENS
            target-servers:
              - lobby
            hologram:
              enabled: true
              start-height: 2.5
              layouts:
                - joinstate: MAINTENANCE
                  text: '<red>Maintenance'
            pushback:
              enabled: true
              radius: 3.0
              strength: 1.5
              vertical: 0.4
              playSound: minecraft:entity.enderman.teleport
            actions:
              - interaction-type: RIGHT_CLICK
                joinstate: DEFAULT
                sendMessage: '<green>Hello'
            """.trimIndent()
        )

        val repository = NpcRepository(directory, NpcProviderRegistry(emptyList()))
        val config = assertNotNull(repository.load().singleOrNull())

        assertEquals(ConfigVersion.VERSION, config.version)
        assertEquals("test", config.provider.reference)
        assertEquals(listOf("lobby"), config.targetServers)
        assertEquals("maintenance", config.hologram.layouts.single().joinState)
        assertEquals("<red>Maintenance", config.hologram.layouts.single().lines.single().text)
        assertEquals("default", config.actions.single().joinState)
        assertEquals("<green>Hello", config.actions.single().sendMessage)
        assertTrue(config.pushback.enabled)
    }
}
