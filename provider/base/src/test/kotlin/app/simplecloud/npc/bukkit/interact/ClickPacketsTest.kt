package app.simplecloud.npc.bukkit.interact

import app.simplecloud.npc.core.interaction.PlayerInteraction
import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClickPacketsTest {

    @Test
    fun `attack action is a left click`() {
        val interaction = ClickPackets.interactionOf(InteractAction.ATTACK, InteractionHand.MAIN_HAND)
        assertEquals(PlayerInteraction.LEFT_CLICK, interaction)
    }

    @Test
    fun `main-hand interact-at is the one right click`() {
        val interaction = ClickPackets.interactionOf(InteractAction.INTERACT_AT, InteractionHand.MAIN_HAND)
        assertEquals(PlayerInteraction.RIGHT_CLICK, interaction)
    }

    @Test
    fun `off-hand and plain interact packets of the same right click are not dispatched`() {
        assertNull(ClickPackets.interactionOf(InteractAction.INTERACT_AT, InteractionHand.OFF_HAND))
        assertNull(ClickPackets.interactionOf(InteractAction.INTERACT, InteractionHand.MAIN_HAND))
        assertNull(ClickPackets.interactionOf(InteractAction.INTERACT, InteractionHand.OFF_HAND))
    }
}
