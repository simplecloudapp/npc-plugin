package app.simplecloud.npc.common.action

import app.simplecloud.npc.common.FakePlayer
import app.simplecloud.npc.common.inventory.source.InventoryOpenContext
import app.simplecloud.npc.common.inventory.view.InventoryOpener
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.repository.NpcRepository
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InteractionExecutorTest {

    private object NoInventories : InventoryOpener {
        override fun open(player: NpcPlayer, inventoryId: String, context: InventoryOpenContext) = Unit
    }

    private fun executor() =
        InteractionExecutor(NpcRepository(createTempDirectory("interaction-executor")), NoInventories)

    @Test
    fun `everything the player sees runs before they are sent away`() = runBlocking {
        val player = FakePlayer()
        val action = NpcConfig.ActionConfiguration(
            playSound = "ENTITY_PLAYER_LEVELUP",
            sendMessage = "Bye",
            sendToServer = "lobby-1",
        )

        executor().executeAction(emptyList(), action, player)

        assertEquals(listOf("sound:ENTITY_PLAYER_LEVELUP", "message", "send:lobby-1"), player.calls)
    }

    @Test
    fun `a failing action does not stop the ones after it`() = runBlocking {
        val player = FakePlayer().apply { soundFails = true }
        val action = NpcConfig.ActionConfiguration(playSound = "broken", sendMessage = "Still here")

        executor().executeAction(emptyList(), action, player)

        assertTrue("message" in player.calls, player.calls.toString())
    }

    @Test
    fun `joining with nowhere to go sends nobody and leaves the message to the offline action`() = runBlocking {
        val player = FakePlayer()

        executor().executeAction(emptyList(), NpcConfig.ActionConfiguration(joinTarget = true), player)

        assertTrue(player.calls.none { it.startsWith("send:") }, player.calls.toString())
        assertTrue(player.messages.isEmpty(), player.messages.toString())
    }

    @Test
    fun `without the permission nothing runs and the player is told`() = runBlocking {
        val player = FakePlayer()
        val action = NpcConfig.ActionConfiguration(
            permission = "vip.join",
            sendMessage = "Welcome",
            sendToServer = "vip-1",
        )

        executor().executeAction(emptyList(), action, player)
        assertEquals(listOf("message"), player.calls)

        player.calls.clear()
        player.permissions += "vip.join"
        executor().executeAction(emptyList(), action, player)
        assertEquals(listOf("message", "send:vip-1"), player.calls)
    }
}
