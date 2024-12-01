package app.simplecloud.npc.shared.config

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Niklas Nieberler
 */

@ConfigSerializable
data class NpcConfig(
    val id: String = "",
    val actions: List<NpcInteraction> = mutableListOf(),
    val options: List<NpcOption> = mutableListOf()
) {

    @ConfigSerializable
    data class NpcInteraction(
        val playerInteraction: PlayerInteraction = PlayerInteraction.LEFT_CLICK,
        val action: Action = Action.OPEN_INVENTORY,
        val options: List<NpcOption> = listOf()
    )

    fun getPlayerInteraction(playerInteraction: PlayerInteraction): NpcInteraction? {
        return this.actions.firstOrNull { it.playerInteraction == playerInteraction }
    }

}