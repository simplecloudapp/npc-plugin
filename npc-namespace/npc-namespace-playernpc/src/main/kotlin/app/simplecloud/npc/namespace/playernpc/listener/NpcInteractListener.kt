package app.simplecloud.npc.namespace.playernpc.listener

import app.simplecloud.npc.namespace.playernpc.PlayerNPCNamespace
import app.simplecloud.npc.namespace.playernpc.option.PlayerNPCOptionProviders
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import dev.sergiferry.playernpc.api.NPC
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * @author Niklas Nieberler
 */

class NpcInteractListener(
    private val namespace: PlayerNPCNamespace
) : Listener {

    @EventHandler
    fun handleInteract(event: NPC.Events.Interact) {
        val npc = event.npc
        val player = event.player

        val interactionExecutor = this.namespace.interactionExecutor
        val optionProvider = PlayerNPCOptionProviders.createInteractOptionProviders(npc)
        interactionExecutor.execute(npc.simpleID, player, getPlayerInteraction(event), optionProvider)
    }

    private fun getPlayerInteraction(event: NPC.Events.Interact): PlayerInteraction {
        return if (event.isRightClick) {
            PlayerInteraction.RIGHT_CLICK
        } else {
            PlayerInteraction.LEFT_CLICK
        }
    }

}