package app.simplecloud.npc.core.config.migration.legacy

import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.interaction.PlayerInteraction
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
data class LegacyNpcConfigV4(
    val id: String = "",
    val hologramConfiguration: LegacyHologramRootV4 = LegacyHologramRootV4(),
    val actions: MutableList<LegacyInteractionV4> = mutableListOf(),
    val options: HashMap<String, String> = hashMapOf(),
)

@ConfigSerializable
data class LegacyInteractionV4(
    val playerInteraction: PlayerInteraction = PlayerInteraction.LEFT_CLICK,
    val action: String = LegacyAction.RUN_COMMAND.name,
    val options: HashMap<String, String> = hashMapOf(),
)

@ConfigSerializable
data class LegacyHologramRootV4(
    val placeholderServerBaseName: String = "",
    val placeholderName: String = "",
    val placeholderGroupName: String = "",
    val holograms: List<LegacyHologramV4> = emptyList(),
)

@ConfigSerializable
data class LegacyHologramV4(
    val startHeight: Double = 2.073,
    val joinState: String = "",
    val lores: List<HologramLine> = emptyList(),
)
