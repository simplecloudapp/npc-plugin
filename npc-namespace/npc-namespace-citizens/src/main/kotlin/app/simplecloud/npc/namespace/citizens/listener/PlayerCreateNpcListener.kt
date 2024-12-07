package app.simplecloud.npc.namespace.citizens.listener

import app.simplecloud.npc.namespace.citizens.CitizensNamespace
import net.citizensnpcs.api.event.PlayerCreateNPCEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * @author Niklas Nieberler
 */

class PlayerCreateNpcListener(
    private val namespace: CitizensNamespace
) : Listener {

    @EventHandler
    fun handleNpcCreate(event: PlayerCreateNPCEvent) {
        val npc = event.npc
        this.namespace.npcManager.create(npc.id.toString())
    }

}