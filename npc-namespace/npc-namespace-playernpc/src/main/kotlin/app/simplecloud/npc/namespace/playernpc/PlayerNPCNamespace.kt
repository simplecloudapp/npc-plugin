package app.simplecloud.npc.namespace.playernpc

import app.simplecloud.npc.namespace.playernpc.listener.NpcInteractListener
import app.simplecloud.npc.shared.event.EventActionType
import app.simplecloud.npc.shared.event.registerActionEvent
import app.simplecloud.npc.shared.namespace.NpcNamespace
import dev.sergiferry.playernpc.api.NPC
import dev.sergiferry.playernpc.api.NPCLib
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class PlayerNPCNamespace : NpcNamespace(
    "PlayerNPC"
) {

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        pluginManager.registerEvents(NpcInteractListener(this), plugin)

        eventManager.registerActionEvent<NPC.Events.Show>(plugin, EventActionType.CREATE, { it.npc.simpleID })
        eventManager.registerActionEvent<NPC.Events.Hide>(plugin, EventActionType.REMOVE, { it.npc.simpleID })
    }

    override fun findAllNpcs(): List<String> {
        return NPCLib.getInstance().allGlobalNPCs.map { it.simpleID }
    }

    override fun findLocationByNpc(id: String): Location? {
        return NPCLib.getInstance().allGlobalNPCs.firstOrNull { it.simpleID == id }?.location
    }

}