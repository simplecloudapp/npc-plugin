package app.simplecloud.npc.core.config.migration

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.repository.NpcRepository
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NpcConfigMigrationTest {

    private fun load(yaml: String): NpcConfig {
        val directory = createTempDirectory("npc-config-migration")
        val trimmed = yaml.trimIndent()
        val file = directory.resolve("${idOf(trimmed)}.yml").apply { writeText(trimmed) }
        val config = assertNotNull(NpcRepository(directory).load().singleOrNull())

        val written = file.readText()
        assertTrue(written.contains("version: ${NpcConfig.CURRENT_VERSION}"), written)
        assertFalse(written.contains("ownership"), written)
        assertFalse(written.contains("hologram-configuration"), written)

        return config
    }

    private fun idOf(yaml: String): String =
        Regex("^id: '?([^'\n]+)'?$", RegexOption.MULTILINE).find(yaml)?.groupValues?.get(1)
            ?: error("fixture has no top-level id")

    @Test
    fun `migrates a version five config written by release 0_1_4`() {
        val config = load(
            """
            version: 5
            id: lobby
            provider:
              type: CITIZENS
              reference: '42'
              ownership: MANAGED
            target-servers:
              - Lobby
            hologram:
              enabled: true
              start-height: 2.073
              layouts:
                - join-state: default
                  lines:
                    - text: '<gray>Lobby'
                      billboard: CENTER
                      alignment: CENTER
                - join-state: maintenance
                  lines:
                    - text: '<red>Maintenance'
            pushback:
              enabled: true
              radius: 1.2
              strength: 0.8
              vertical: 0.3
            actions:
              - interaction-type: RIGHT_CLICK
                join-state: DEFAULT
                join-target: true
              - interaction-type: LEFT_CLICK
                join-state: maintenance
                send-message: '<red>Closed'
            """,
        )

        assertEquals(NpcConfig.CURRENT_VERSION, config.version)
        assertEquals("citizens", config.entity.provider)
        assertEquals("42", config.entity.providerReference)
        assertTrue(config.entity.needsRelocation)
        assertEquals(listOf("Lobby"), config.targetServers)

        assertEquals(listOf("public", "maintenance"), config.hologram.layouts.map { it.joinState })
        assertEquals("<gray>Lobby", config.hologram.layoutShownFor(null)?.lines?.single()?.text)
        assertEquals("<gray>Lobby", config.hologram.layoutShownFor("public")?.lines?.single()?.text)

        assertEquals(listOf("public", "maintenance"), config.actions.map { it.joinState })
        assertTrue(config.actionsFor(PlayerInteraction.RIGHT_CLICK, null).single().joinTarget)
        assertTrue(config.actionsFor(PlayerInteraction.RIGHT_CLICK, "public").single().joinTarget)
        assertEquals("<red>Closed", config.actionsFor(PlayerInteraction.LEFT_CLICK, "maintenance").single().sendMessage)
        assertTrue(config.pushback.enabled)
    }

    @Test
    fun `keeps a linked version five npc linked`() {
        val config = load(
            """
            version: 5
            id: shop
            provider:
              type: FANCY_NPCS
              reference: shopkeeper
              ownership: LINKED
            target-servers:
              - Shop
            """,
        )

        assertEquals("fancynpcs", config.entity.provider)
        assertEquals("shopkeeper", config.entity.providerReference)
        assertTrue(config.entity.providerLinked)
        assertFalse(config.entity.needsRelocation)
    }

    @Test
    fun `migrates a version four config written by release 0_1_3 and older`() {
        val config = load(
            """
            version: '4'
            id: '12'
            hologram-configuration:
                placeholder-group-name: Lobby
                holograms:
                -   start-height: 2.2
                    join-state: ''
                    lores:
                    -   text: '<gold>Lobby'
                        billboard: CENTER
                        alignment: CENTER
            actions:
            -   player-interaction: RIGHT_CLICK
                action: QUICK_JOIN
                options: {}
            options:
                group.name: Lobby
            """,
        )

        assertEquals(NpcConfig.CURRENT_VERSION, config.version)
        assertEquals("standalone", config.entity.provider)
        assertNull(config.entity.providerReference)
        assertTrue(config.entity.needsRelocation)
        assertEquals(listOf("Lobby"), config.targetServers)
        assertEquals(2.2, config.hologram.startHeight)
        assertEquals("public", config.hologram.layouts.single().joinState)
        assertEquals("<gold>Lobby", config.hologram.layouts.single().lines.single().text)
        assertTrue(config.actionsFor(PlayerInteraction.RIGHT_CLICK, null).single().joinTarget)
    }

    @Test
    fun `migrates a proposed version one config`() {
        val config = load(
            """
            version: 1
            id: lobby
            provider: FANCY_NPCS
            target-servers:
              - Lobby
            hologram:
              enabled: true
              start-height: 2.5
              layouts:
                - joinstate: default
                  text: '<gold>Lobby'
                - joinstate: Maintenance
                  text: '<red>Closed'
            pushback:
              enabled: true
              radius: 3.0
              strength: 1.5
              vertical: 0.4
              playSound: entity.player.hurt
            actions:
              - interaction-type: RIGHT_CLICK
                joinstate: default
                sendMessage: hi
                teleport:
                  enabled: true
                  world: hub
                  x: 1.0
                  y: 2.0
                  z: 3.0
                sendTitle:
                  enabled: false
                  title: ignored
              - interaction-type: LEFT_CLICK
                joinstate: maintenance
                sendToServer: Lobby-1
            """,
        )

        assertEquals(NpcConfig.CURRENT_VERSION, config.version)
        assertEquals("fancynpcs", config.entity.provider)
        assertEquals("lobby", config.entity.providerReference)
        assertTrue(config.entity.needsRelocation)
        assertEquals(listOf("Lobby"), config.targetServers)
        assertEquals(2.5, config.hologram.startHeight)
        assertEquals(listOf("public", "maintenance"), config.hologram.layouts.map { it.joinState })
        assertEquals("<red>Closed", config.hologram.layoutShownFor("maintenance")?.lines?.single()?.text)
        assertTrue(config.pushback.enabled)
        assertEquals("entity.player.hurt", config.pushback.sound)

        val right = config.actionsFor(PlayerInteraction.RIGHT_CLICK, null).single()
        assertEquals("hi", right.sendMessage)
        assertEquals("hub", right.teleport?.world)
        assertEquals(3.0, right.teleport?.z)
        assertNull(right.sendTitle, "a disabled title is dropped")
        assertEquals("Lobby-1", config.actionsFor(PlayerInteraction.LEFT_CLICK, "maintenance").single().sendToServer)
    }

    @Test
    fun `keeps current configs untouched and writes the current version back`() {
        val directory = createTempDirectory("npc-config-roundtrip")
        val repository = NpcRepository(directory)
        repository.save(
            NpcConfig(
                id = "hub",
                entity = NpcConfig.NpcEntityConfiguration(provider = "standalone"),
                targetServers = mutableListOf("Hub"),
                actions = mutableListOf(
                    NpcConfig.ActionConfiguration(PlayerInteraction.RIGHT_CLICK, "PUBLIC", joinTarget = true),
                ),
            ),
        )

        val yaml = directory.resolve("hub.yml").readText()
        assertTrue(yaml.contains("version: ${NpcConfig.CURRENT_VERSION}"))
        assertTrue(yaml.contains("join-state: public"))

        val reloaded = assertNotNull(NpcRepository(directory).load().singleOrNull())
        assertEquals("standalone", reloaded.entity.provider)
        assertFalse(reloaded.entity.needsRelocation)
        assertEquals(listOf("Hub"), reloaded.targetServers)
    }
}
