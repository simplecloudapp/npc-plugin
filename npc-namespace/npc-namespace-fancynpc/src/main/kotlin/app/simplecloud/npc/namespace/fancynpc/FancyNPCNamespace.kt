package app.simplecloud.npc.namespace.fancynpc

import app.simplecloud.npc.shared.namespace.NpcNamespace
import de.oliver.fancynpcs.api.Npc
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class FancyNPCNamespace : NpcNamespace<Npc>(
    "FancyNPC",
    { it.data.id }
) {

    override fun onEnable() {
        TODO("Not yet implemented")
    }

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        TODO("Not yet implemented")
    }

}