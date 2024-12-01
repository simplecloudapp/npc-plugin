package app.simplecloud.npc.namespace.citizens.listener

import app.simplecloud.npc.namespace.citizens.CitizensNamespace
import net.citizensnpcs.api.event.NPCRemoveEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * @author Niklas Nieberler
 */

class NpcRemoveListener(
    private val namespace: CitizensNamespace
) : Listener {

    @EventHandler
    fun handleNpcRemove(event: NPCRemoveEvent) {
        val npc = event.npc
        this.namespace.npcManager.delete(npc)
    }

}