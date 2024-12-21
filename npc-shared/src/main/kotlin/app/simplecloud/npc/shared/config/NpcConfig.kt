package app.simplecloud.npc.shared.config

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.hologram.config.HologramConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Niklas Nieberler
 */

@ConfigSerializable
data class NpcConfig(
    val id: String = "",
    val hologram: NpcHologram = NpcHologram(),
    val actions: MutableList<NpcInteraction> = mutableListOf(),
    val options: MutableList<NpcOption> = mutableListOf()
) {

    @ConfigSerializable
    data class NpcInteraction(
        val playerInteraction: PlayerInteraction = PlayerInteraction.LEFT_CLICK,
        var action: Action = Action.OPEN_INVENTORY,
        val options: MutableList<NpcOption> = mutableListOf()
    ) {
        fun getOption(key: String): NpcOption? {
            return this.options.firstOrNull { it.key == key }
        }
    }

    @ConfigSerializable
    data class NpcHologram(
        val startHeight: Float = 1.1F,
        val holograms: List<HologramConfig> = emptyList()
    )

    fun getOption(key: String): NpcOption? {
        return this.options.firstOrNull { it.key == key }
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