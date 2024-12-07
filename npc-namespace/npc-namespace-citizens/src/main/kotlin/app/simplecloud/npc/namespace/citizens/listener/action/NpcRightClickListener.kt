package app.simplecloud.npc.namespace.citizens.listener.action

import app.simplecloud.npc.namespace.citizens.CitizensNamespace
import app.simplecloud.npc.namespace.citizens.option.CitizensOptionProviders
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import net.citizensnpcs.api.event.NPCRightClickEvent

/**
 * @author Niklas Nieberler
 */

class NpcRightClickListener(
    private val namespace: CitizensNamespace
) : Listener {

    @EventHandler
    fun handleNpcRightClick(event: NPCRightClickEvent) {
        val npc = event.npc
        val player = event.clicker

        val interactionExecutor = this.namespace.interactionExecutor
        val optionProvider = CitizensOptionProviders.createInteractOptionProviders(npc)
        interactionExecutor.execute(npc.id.toString(), player, PlayerInteraction.RIGHT_CLICK, optionProvider)
    }

}