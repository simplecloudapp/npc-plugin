package app.simplecloud.npc.namespace.fancynpcs

import app.simplecloud.npc.namespace.fancynpcs.listener.NpcInteractListener
import app.simplecloud.npc.shared.event.EventActionType
import app.simplecloud.npc.shared.event.registerActionEvent
import app.simplecloud.npc.shared.namespace.NpcNamespace
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.events.NpcCreateEvent
import de.oliver.fancynpcs.api.events.NpcRemoveEvent
import de.oliver.fancynpcs.api.events.NpcSpawnEvent
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class FancyNpcsNamespace : NpcNamespace(
    "FancyNpcs"
) {

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        pluginManager.registerEvents(NpcInteractListener(this), plugin)

        eventManager.registerActionEvent<NpcCreateEvent>(plugin, EventActionType.SPAWN, { it.npc.data.name })
        eventManager.registerActionEvent<NpcRemoveEvent>(plugin, EventActionType.REMOVE, { it.npc.data.name })
    }

    override fun findAllNpcs(): List<String> {
        return FancyNpcsPlugin.get().npcManager.allNpcs.map { it.data.name }
    }

    override fun findLocationByNpc(id: String): Location? {
        return FancyNpcsPlugin.get().npcManager.allNpcs
            .map { it.data }
            .firstOrNull { it.name == id }?.location
    }

}