package app.simplecloud.npc.bukkit.interact

import app.simplecloud.npc.core.interaction.PlayerInteraction
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction

object ClickPackets {

    class Click(val entityId: Int, val interaction: PlayerInteraction?)

    fun read(event: PacketReceiveEvent): Click? = when (event.packetType) {
        PacketType.Play.Client.ATTACK -> Click(WrapperPlayClientAttack(event).entityId, PlayerInteraction.LEFT_CLICK)
        PacketType.Play.Client.INTERACT_ENTITY -> {
            val wrapper = WrapperPlayClientInteractEntity(event)
            Click(wrapper.entityId, interactionOf(wrapper.action, wrapper.hand))
        }

        else -> null
    }

    fun interactionOf(action: InteractAction, hand: InteractionHand): PlayerInteraction? = when (action) {
        InteractAction.ATTACK -> PlayerInteraction.LEFT_CLICK
        InteractAction.INTERACT_AT -> if (hand == InteractionHand.MAIN_HAND) PlayerInteraction.RIGHT_CLICK else null
        InteractAction.INTERACT -> null
    }
}
