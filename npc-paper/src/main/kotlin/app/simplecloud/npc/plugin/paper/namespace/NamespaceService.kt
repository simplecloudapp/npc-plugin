package app.simplecloud.npc.plugin.paper.namespace

import app.simplecloud.npc.namespace.citizens.CitizensNamespace
import app.simplecloud.npc.shared.namespace.NpcNamespace
import org.bukkit.Bukkit

/**
 * @author Niklas Nieberler
 */

object NamespaceService {

    private val spaces = arrayOf<NpcNamespace<out Any>>(
        CitizensNamespace()
    )

    /**
     * Check with the [org.bukkit.plugin.PluginManager] if an available npc plugin with an existing namespace was found
     * @return the best possible [NpcNamespace]
     */
    fun findPossibleNamespace(): NpcNamespace<out Any>? {
        return this.spaces.firstOrNull { getAvailablePluginNames().contains(it.pluginName) }
    }

    private fun getAvailablePluginNames(): List<String> {
        return Bukkit.getPluginManager().plugins.map { it.name }
    }

}