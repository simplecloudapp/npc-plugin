package app.simplecloud.npc.namespace.playernpc

import app.simplecloud.npc.shared.namespace.NpcNamespace
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class PlayerNPCNamespace : NpcNamespace(
    "PlayerNPC"
) {

    override fun onEnable() {
        TODO("Not yet implemented")
    }

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        TODO("Not yet implemented")
    }

    override fun findAllNpcs(): List<String> {
        TODO("Not yet implemented")
    }

}