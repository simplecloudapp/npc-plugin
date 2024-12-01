package app.simplecloud.npc.namespace.playernpc

import app.simplecloud.npc.shared.namespace.NpcNamespace
import dev.sergiferry.playernpc.api.NPC
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class PlayerNPCNamespace : NpcNamespace<NPC>(
    "PlayerNPC",
    { it.simpleID }
) {

    override fun onEnable() {
        TODO("Not yet implemented")
    }

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        TODO("Not yet implemented")
    }

}