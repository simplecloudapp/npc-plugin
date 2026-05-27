package app.simplecloud.npc.namespace.znpcsplus.listener

import app.simplecloud.npc.namespace.znpcsplus.ZNpcsPlusNamespace
import app.simplecloud.npc.namespace.znpcsplus.option.ZNpcsPlusOptionProviders
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import lol.pyr.znpcsplus.api.event.NpcInteractEvent
import lol.pyr.znpcsplus.api.interaction.InteractionType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * @author Niklas Nieberler
 */

class NpcInteractListener(
    private val namespace: ZNpcsPlusNamespace
) : Listener {

    @EventHandler
    fun handleInteract(event: NpcInteractEvent) {
        val npc = event.entry
        val player = event.player

        val interactionExecutor = this.namespace.interactionExecutor
        val optionProvider = ZNpcsPlusOptionProviders.createInteractOptionProviders(npc)
        interactionExecutor.execute(npc.id, player, getPlayerInteraction(event), optionProvider)
    }

    private fun getPlayerInteraction(event: NpcInteractEvent): PlayerInteraction {
        return if (event.clickType == InteractionType.RIGHT_CLICK) {
            PlayerInteraction.RIGHT_CLICK
        } else {
            PlayerInteraction.LEFT_CLICK
        }
    }

}