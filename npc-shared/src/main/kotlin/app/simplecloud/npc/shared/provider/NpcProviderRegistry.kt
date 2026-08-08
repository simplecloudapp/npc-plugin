package app.simplecloud.npc.shared.provider

import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

class NpcProviderRegistry(
    providers: Collection<NpcProvider>,
) {
    private val providers = providers.associateBy { it.type }

    fun availableProviders(pluginManager: PluginManager): List<NpcProvider> {
        return providers.values.filter { it.isAvailable(pluginManager) }
    }

    fun get(type: NpcProviderType): NpcProvider? = providers[type]

    fun getAvailable(type: NpcProviderType, pluginManager: PluginManager): NpcProvider? {
        return providers[type]?.takeIf { it.isAvailable(pluginManager) }
    }

    fun enable(pluginManager: PluginManager, plugin: Plugin, events: NpcProviderEvents) {
        availableProviders(pluginManager).forEach {
            it.onEnable()
            it.registerListeners(pluginManager, plugin, events)
        }
    }

    fun disable(pluginManager: PluginManager) {
        availableProviders(pluginManager).forEach(NpcProvider::onDisable)
    }
}
