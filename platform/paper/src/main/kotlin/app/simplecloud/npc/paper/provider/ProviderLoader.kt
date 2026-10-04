package app.simplecloud.npc.paper.provider

import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.core.render.NpcRenderer
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

object ProviderLoader {
    fun load(plugin: Plugin, onInteract: OnNpcInteract, catalog: List<ProviderDescriptor>): Map<String, NpcRenderer> =
        buildMap {
            catalog.forEach { descriptor ->
                val required = descriptor.requiredPlugin
                if (required != null && Bukkit.getPluginManager().getPlugin(required)?.isEnabled != true) return@forEach
                if (!descriptor.gate()) return@forEach

                descriptor.create(plugin, onInteract)?.let { put(descriptor.name, it) }
            }
        }
}
