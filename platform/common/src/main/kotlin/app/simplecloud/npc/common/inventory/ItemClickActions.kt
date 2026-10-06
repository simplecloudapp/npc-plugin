package app.simplecloud.npc.common.inventory

import app.simplecloud.npc.core.config.NpcConfig.ActionConfiguration
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration

object ItemClickActions {

    fun of(item: InventoryItemConfiguration, click: PlayerInteraction): ActionConfiguration? =
        item.actions.firstOrNull { it.interactionType == click }

    fun orCreate(item: InventoryItemConfiguration, click: PlayerInteraction): ActionConfiguration =
        of(item, click) ?: ActionConfiguration(interactionType = click).also(item.actions::add)

    fun forClick(actions: List<ActionConfiguration>, click: PlayerInteraction): List<ActionConfiguration> =
        actions.filter { it.interactionType == click }
            .ifEmpty { actions.filter { it.interactionType == click.unshifted() } }
            .ifEmpty {
                if (actions.all { it.interactionType == PlayerInteraction.RIGHT_CLICK }) actions else emptyList()
            }

    private fun PlayerInteraction.unshifted(): PlayerInteraction = when (this) {
        PlayerInteraction.SHIFT_LEFT_CLICK -> PlayerInteraction.LEFT_CLICK
        PlayerInteraction.SHIFT_RIGHT_CLICK -> PlayerInteraction.RIGHT_CLICK
        else -> this
    }
}
