package app.simplecloud.npc.plugin.paper.namespace

import app.simplecloud.npc.namespace.citizens.CitizensNamespace
import app.simplecloud.npc.namespace.fancynpcs.FancyNpcsNamespace
import app.simplecloud.npc.namespace.mythicmobs.MythicMobsNamespace
import app.simplecloud.npc.namespace.playernpc.PlayerNPCNamespace
import app.simplecloud.npc.shared.namespace.NpcNamespace
import org.bukkit.Bukkit

/**
 * @author Niklas Nieberler
 */

object NamespaceService {

    private val spaces = arrayOf<NpcNamespace>(
        CitizensNamespace(),
        PlayerNPCNamespace(),
        FancyNpcsNamespace(),
        MythicMobsNamespace()
    )

    /**
     * Check with the [org.bukkit.plugin.PluginManager] if an available npc plugin with an existing namespace was found
     * @return the best possible [NpcNamespace]
     */
    fun findPossibleNamespace(): NpcNamespace? {
        return this.spaces.firstOrNull { getAvailablePluginNames().contains(it.pluginName) }
    }

    private fun getAvailablePluginNames(): List<String> {
        val pluginManager = Bukkit.getPluginManager()
        return pluginManager.plugins
            .map { it.name }
            .filter { pluginManager.isPluginEnabled(it) }
    }

}