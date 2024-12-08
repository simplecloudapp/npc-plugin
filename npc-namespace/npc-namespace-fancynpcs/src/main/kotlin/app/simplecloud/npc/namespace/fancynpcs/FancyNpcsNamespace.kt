package app.simplecloud.npc.namespace.fancynpcs

import app.simplecloud.npc.namespace.fancynpcs.listener.NpcInteractListener
import app.simplecloud.npc.shared.event.EventActionType
import app.simplecloud.npc.shared.event.registerActionEvent
import app.simplecloud.npc.shared.namespace.NpcNamespace
import de.oliver.fancynpcs.api.events.NpcRemoveEvent
import de.oliver.fancynpcs.api.events.NpcSpawnEvent
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class FancyNpcsNamespace : NpcNamespace(
    "FancyNpcs"
) {

    override fun onEnable() {}

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        pluginManager.registerEvents(NpcInteractListener(this), plugin)

        eventManager.registerActionEvent<NpcSpawnEvent>(plugin, EventActionType.SPAWN, { it.npc.data.id })
        eventManager.registerActionEvent<NpcRemoveEvent>(plugin, EventActionType.REMOVE, { it.npc.data.id })
    }

}