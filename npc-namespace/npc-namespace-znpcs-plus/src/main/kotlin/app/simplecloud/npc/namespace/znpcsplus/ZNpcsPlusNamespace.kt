package app.simplecloud.npc.namespace.znpcsplus

import app.simplecloud.npc.namespace.znpcsplus.listener.NpcInteractListener
import app.simplecloud.npc.shared.event.EventActionType
import app.simplecloud.npc.shared.event.registerActionEvent
import app.simplecloud.npc.shared.namespace.NpcNamespace
import lol.pyr.znpcsplus.api.NpcApiProvider
import lol.pyr.znpcsplus.api.event.NpcDespawnEvent
import lol.pyr.znpcsplus.api.event.NpcSpawnEvent
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class ZNpcsPlusNamespace : NpcNamespace(
    "ZNPCsPlus"
) {

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        pluginManager.registerEvents(NpcInteractListener(this), plugin)
        eventManager.registerActionEvent<NpcSpawnEvent>(plugin, EventActionType.SPAWN, { it.entry.id })
        eventManager.registerActionEvent<NpcDespawnEvent>(plugin, EventActionType.REMOVE, { it.entry.id })
    }

    override fun findAllNpcs(): List<String> {
        return NpcApiProvider.get().npcRegistry.allIds.toList()
    }

    override fun findLocationByNpc(id: String): Location? {
        val npc = NpcApiProvider.get().npcRegistry.getById(id).npc
        return npc.location.toBukkitLocation(npc.world)
    }

}