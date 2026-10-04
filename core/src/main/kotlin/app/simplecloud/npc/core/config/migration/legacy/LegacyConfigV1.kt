package app.simplecloud.npc.core.config.migration.legacy

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.interaction.PlayerInteraction
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class LegacyNpcConfigV1(
    val id: String = "",
    val provider: String = "CITIZENS",
    @Setting("target-servers")
    val targetServers: List<String> = emptyList(),
    val hologram: LegacyHologramV1 = LegacyHologramV1(),
    val pushback: LegacyPushbackV1 = LegacyPushbackV1(),
    val actions: List<LegacyActionV1> = emptyList(),
)

@ConfigSerializable
data class LegacyHologramV1(
    val enabled: Boolean = true,
    @Setting("start-height")
    val startHeight: Double = 2.073,
    val layouts: List<LegacyLayoutV1> = emptyList(),
)

@ConfigSerializable
data class LegacyLayoutV1(
    @Setting("joinstate")
    val joinState: String = NpcConfig.DEFAULT_JOIN_STATE,
    val text: String = "",
)

@ConfigSerializable
data class LegacyPushbackV1(
    val enabled: Boolean = false,
    val radius: Double = 2.5,
    val strength: Double = 1.2,
    val vertical: Double = 0.3,
    @Setting("playSound")
    val playSound: String? = null,
)

@ConfigSerializable
data class LegacyActionV1(
    @Setting("interaction-type")
    val interactionType: PlayerInteraction = PlayerInteraction.RIGHT_CLICK,
    @Setting("joinstate")
    val joinState: String = NpcConfig.DEFAULT_JOIN_STATE,
    @Setting("openInventory")
    val openInventory: String? = null,
    @Setting("playSound")
    val playSound: String? = null,
    @Setting("executeCommand")
    val executeCommand: String? = null,
    @Setting("sendMessage")
    val sendMessage: String? = null,
    val teleport: LegacyTeleportV1? = null,
    @Setting("sendTitle")
    val sendTitle: LegacyTitleV1? = null,
    @Setting("sendToServer")
    val sendToServer: String? = null,
    @Setting("transferToServer")
    val transferToServer: String? = null,
)

@ConfigSerializable
data class LegacyTeleportV1(
    val enabled: Boolean = false,
    val world: String = "world",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val yaw: Float = 0F,
    val pitch: Float = 0F,
)

@ConfigSerializable
data class LegacyTitleV1(
    val enabled: Boolean = false,
    val title: String = "",
    val text: String = "",
    @Setting("fadeIn")
    val fadeIn: Int = 20,
    val stay: Int = 20,
    @Setting("fadeOut")
    val fadeOut: Int = 20,
)
