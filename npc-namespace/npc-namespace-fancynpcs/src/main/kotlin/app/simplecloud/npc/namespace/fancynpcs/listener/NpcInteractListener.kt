package app.simplecloud.npc.namespace.fancynpcs.listener

import app.simplecloud.npc.namespace.fancynpcs.FancyNpcsNamespace
import app.simplecloud.npc.namespace.fancynpcs.option.FancyNpcsOptionProviders
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import de.oliver.fancynpcs.api.actions.ActionTrigger
import de.oliver.fancynpcs.api.events.NpcInteractEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * @author Niklas Nieberler
 */

class NpcInteractListener(
    private val namespace: FancyNpcsNamespace
) : Listener {

    @EventHandler
    fun handleInteract(event: NpcInteractEvent) {
        val npc = event.npc
        val player = event.player

        val interactionExecutor = this.namespace.interactionExecutor
        val optionProvider = FancyNpcsOptionProviders.createInteractOptionProviders(npc)
        interactionExecutor.execute(npc.data.id, player, getPlayerInteraction(event), optionProvider)
    }

    private fun getPlayerInteraction(event: NpcInteractEvent): PlayerInteraction {
        return if (event.interactionType == ActionTrigger.RIGHT_CLICK) {
            PlayerInteraction.RIGHT_CLICK
        } else {
            PlayerInteraction.LEFT_CLICK
        }
    }

}