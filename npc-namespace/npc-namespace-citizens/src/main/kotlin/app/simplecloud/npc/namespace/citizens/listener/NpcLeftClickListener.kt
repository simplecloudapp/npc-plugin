package app.simplecloud.npc.namespace.citizens.listener

import app.simplecloud.npc.namespace.citizens.CitizensNamespace
import app.simplecloud.npc.namespace.citizens.option.CitizensOptionProviders
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import net.citizensnpcs.api.event.NPCLeftClickEvent

/**
 * @author Niklas Nieberler
 */

class NpcLeftClickListener(
    private val namespace: CitizensNamespace
) : Listener {

    @EventHandler
    fun handleNpcLeftClick(event: NPCLeftClickEvent) {
        val npc = event.npc
        val player = event.clicker

        val interactionExecutor = this.namespace.interactionExecutor
        val optionProvider = CitizensOptionProviders.createInteractOptionProviders(npc)
        interactionExecutor.execute(npc.id.toString(), player, PlayerInteraction.LEFT_CLICK, optionProvider)
    }

}