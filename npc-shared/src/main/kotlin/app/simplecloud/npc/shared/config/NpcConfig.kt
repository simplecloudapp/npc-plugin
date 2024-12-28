package app.simplecloud.npc.shared.config

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.hologram.config.HologramConfiguration
import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.utils.ConfigVersion
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Niklas Nieberler
 */

@ConfigSerializable
data class NpcConfig(
    val version: String = ConfigVersion.version,
    val id: String = "",
    val hologramConfiguration: NpcHologramConfiguration = NpcHologramConfiguration(),
    val actions: MutableList<NpcInteraction> = mutableListOf(),
    val options: HashMap<String, String> = hashMapOf()
) {

    @ConfigSerializable
    data class NpcInteraction(
        val playerInteraction: PlayerInteraction = PlayerInteraction.LEFT_CLICK,
        var action: Action = Action.OPEN_INVENTORY,
        val options: HashMap<String, String> = hashMapOf()
    ) {
        fun getOption(key: String): Option? {
            return this.options[key]?.let { Option(key, it) }
        }

        fun getOptions(): List<Option> {
            return this.options.map { Option(it.key, it.value) }
        }
    }

    @ConfigSerializable
    data class NpcHologramConfiguration(
        var placeholderGroupName: String = "lobby",
        val holograms: List<NpcHologram> = mutableListOf(),
    ) {
        fun getHologram(joinState: String?): NpcHologram {
            return this.holograms.firstOrNull { it.joinState == joinState } ?: getFallbackHologram()
        }

        fun getFallbackHologram(): NpcHologram {
            return this.holograms.firstOrNull { it.joinState.isBlank() }
                ?: throw NullPointerException("failed to find fallback hologram")
        }
    }

    @ConfigSerializable
    data class NpcHologram(
        val startHeight: Double = 2.073,
        val joinState: String = "",
        val lores: List<HologramConfiguration> = emptyList()
    )

    fun getOption(key: String): Option? {
        return this.options[key]?.let { Option(key, it) }
    }

    fun updateAction(npcInteraction: NpcInteraction): NpcConfig {
        this.actions.removeIf { it.playerInteraction == npcInteraction.playerInteraction }
        this.actions.add(npcInteraction)
        return this
    }

    fun getPlayerInteraction(playerInteraction: PlayerInteraction): NpcInteraction? {
        return this.actions.firstOrNull { it.playerInteraction == playerInteraction }
    }

    fun getPlayerInteraction(playerInteraction: String): NpcInteraction? {
        return this.actions.firstOrNull { it.playerInteraction.name.equals(playerInteraction, true) }
    }

}