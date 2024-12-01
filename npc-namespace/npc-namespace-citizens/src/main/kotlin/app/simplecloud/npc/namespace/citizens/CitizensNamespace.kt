package app.simplecloud.npc.namespace.citizens

import app.simplecloud.npc.namespace.citizens.listener.NpcRemoveListener
import app.simplecloud.npc.namespace.citizens.listener.PlayerCreateNpcListener
import app.simplecloud.npc.namespace.citizens.listener.action.NpcLeftClickListener
import app.simplecloud.npc.namespace.citizens.listener.action.NpcRightClickListener
import app.simplecloud.npc.shared.namespace.NpcNamespace
import net.citizensnpcs.api.npc.NPC
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class CitizensNamespace : NpcNamespace<NPC>(
    "Citizens",
    { it.id.toString() }
) {

    override fun onEnable() {

    }

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        pluginManager.registerEvents(NpcRightClickListener(this), plugin)
        pluginManager.registerEvents(NpcLeftClickListener(this), plugin)

        pluginManager.registerEvents(PlayerCreateNpcListener(this), plugin)
        pluginManager.registerEvents(NpcRemoveListener(this), plugin)
    }

}