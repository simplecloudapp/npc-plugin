package app.simplecloud.npc.core.repository

import app.simplecloud.npc.core.config.JoinStrategy
import app.simplecloud.npc.core.config.NpcPose
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class NpcRepositoryFileNameTest {

    private fun config(id: String) = """
        version: 6
        id: $id
        target-servers:
          - Lobby
    """.trimIndent()

    @Test
    fun `a file whose name does not match its id is skipped`() {
        val directory = createTempDirectory("npc-file-name")
        directory.resolve("hub.yml").writeText(config("lobby"))

        assertEquals(emptyList(), NpcRepository(directory).load().map { it.id })
    }

    @Test
    fun `a file without a version in today's layout is not migrated away`() {
        val directory = createTempDirectory("npc-file-unversioned")
        directory.resolve("lobby.yml").writeText(
            """
            id: lobby
            target-servers:
              - Lobby
            """.trimIndent(),
        )

        assertEquals(listOf("Lobby"), NpcRepository(directory).load().single().targetServers)
    }

    @Test
    fun `an unknown enum value falls back to its default instead of dropping the NPC`() {
        val directory = createTempDirectory("npc-file-enum")
        directory.resolve("lobby.yml").writeText(
            """
            version: 6
            id: lobby
            join-strategy: FULLEST
            entity:
              pose: FLYING
            """.trimIndent(),
        )

        val config = NpcRepository(directory).load().single()

        assertEquals(JoinStrategy.LEAST_PLAYERS, config.joinStrategy)
        assertEquals(NpcPose.STANDING, config.entity.pose)
    }
}
