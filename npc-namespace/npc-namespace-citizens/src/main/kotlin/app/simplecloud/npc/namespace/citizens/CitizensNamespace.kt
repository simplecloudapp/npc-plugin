package app.simplecloud.npc.namespace.citizens

import app.simplecloud.npc.namespace.citizens.listener.*
import app.simplecloud.npc.shared.namespace.NpcNamespace
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
    }

}