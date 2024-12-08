package app.simplecloud.npc.namespace.citizens

import app.simplecloud.npc.namespace.citizens.listener.NpcLeftClickListener
import app.simplecloud.npc.namespace.citizens.listener.NpcRightClickListener
import app.simplecloud.npc.shared.event.EventActionType
import app.simplecloud.npc.shared.event.registerActionEvent
import app.simplecloud.npc.shared.namespace.NpcNamespace
import net.citizensnpcs.api.event.*
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class CitizensNamespace : NpcNamespace(
    "Citizens"
) {

    override fun onEnable() {

    }

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        pluginManager.registerEvents(NpcRightClickListener(this), plugin)
        pluginManager.registerEvents(NpcLeftClickListener(this), plugin)

        eventManager.registerActionEvent<PlayerCreateNPCEvent>(plugin, EventActionType.SPAWN, { it.npc.id.toString() })
        eventManager.registerActionEvent<NPCRemoveEvent>(plugin, EventActionType.REMOVE, { it.npc.id.toString() })
    }

    override fun findAllNpcs(): List<String> {
        return listOf() // TODO: here
    }

}