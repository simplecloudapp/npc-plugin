package app.simplecloud.npc.shared.provider

import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.option.OptionProvider
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

interface NpcProvider {
    val type: NpcProviderType
    val pluginName: String
    val supportsCreation: Boolean
        get() = false

    fun isAvailable(pluginManager: PluginManager): Boolean {
        return pluginManager.getPlugin(pluginName)?.isEnabled == true
    }

    fun onEnable() {}
    fun onDisable() {}

    fun registerListeners(pluginManager: PluginManager, plugin: Plugin, events: NpcProviderEvents)

    fun exists(reference: String): Boolean
    fun location(reference: String): Location?
    fun references(): Collection<String> = emptyList()

    fun create(id: String, player: Player, location: Location): String {
        throw UnsupportedOperationException("Provider ${type.commandName} cannot create NPCs")
    }

    fun delete(reference: String): Boolean = false
}

object NpcCreationDefaults {
    const val LOOK_AT_PLAYER_DISTANCE = 10
}

class NpcProviderEvents(
    val interact: (
        provider: NpcProviderType,
        reference: String,
        player: Player,
        interaction: PlayerInteraction,
        options: OptionProvider,
    ) -> Unit,
    val spawn: (provider: NpcProviderType, reference: String) -> Unit,
    val remove: (provider: NpcProviderType, reference: String) -> Unit,
)
