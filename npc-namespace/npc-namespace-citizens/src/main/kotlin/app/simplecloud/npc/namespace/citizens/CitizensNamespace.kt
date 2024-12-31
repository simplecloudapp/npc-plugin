package app.simplecloud.npc.namespace.citizens

import app.simplecloud.npc.namespace.citizens.listener.NpcLeftClickListener
import app.simplecloud.npc.namespace.citizens.listener.NpcRightClickListener
import app.simplecloud.npc.shared.event.EventActionType
import app.simplecloud.npc.shared.event.listener.listenEvent
import app.simplecloud.npc.shared.event.registerActionEvent
import app.simplecloud.npc.shared.namespace.NpcNamespace
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.*
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class CitizensNamespace : NpcNamespace(
    "Citizens"
) {

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        pluginManager.registerEvents(NpcRightClickListener(this), plugin)
        pluginManager.registerEvents(NpcLeftClickListener(this), plugin)

        listenEvent<NPCDespawnEvent>(plugin)
            .addAction { hologramManager.destroyHolograms(it.npc.id.toString()) }

        eventManager.registerActionEvent<NPCRemoveEvent>(plugin, EventActionType.REMOVE, { it.npc.id.toString() })
    }

    override fun findAllNpcs(): List<String> {
        return CitizensAPI.getNPCRegistry().map { it.id.toString() }
    }

    override fun findLocationByNpc(id: String): Location? {
        return CitizensAPI.getNPCRegistry().getById(id.toInt()).storedLocation
    }

}