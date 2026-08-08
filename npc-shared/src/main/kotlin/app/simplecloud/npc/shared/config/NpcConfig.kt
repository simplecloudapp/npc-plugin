package app.simplecloud.npc.shared.config

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.hologram.config.HologramConfiguration
import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.provider.NpcProviderType
import app.simplecloud.npc.shared.provider.ProviderOwnership
import app.simplecloud.npc.shared.utils.ConfigVersion
import app.simplecloud.plugin.api.shared.config.VersionedConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

/**
 * The user-facing NPC configuration.
 *
 * Provider type and reference intentionally live together. The SimpleCloud id is
 * stable and friendly while [ProviderConfiguration.reference] is free to use the
 * native identifier of the selected provider.
 */
@ConfigSerializable
data class NpcConfig(
    override val version: Int = ConfigVersion.VERSION,
    val id: String = "",
    val provider: ProviderConfiguration = ProviderConfiguration(),
    @Setting("target-servers")
    val targetServers: MutableList<String> = mutableListOf(),
    val hologram: HologramConfigurationRoot = HologramConfigurationRoot(),
    val pushback: PushbackConfiguration = PushbackConfiguration(),
    val actions: MutableList<ActionConfiguration> = mutableListOf(),
) : VersionedConfig {

    @ConfigSerializable
    data class ProviderConfiguration(
        val type: NpcProviderType = NpcProviderType.CITIZENS,
        val reference: String = "",
        val ownership: ProviderOwnership = ProviderOwnership.LINKED,
    )

    @ConfigSerializable
    data class HologramConfigurationRoot(
        var enabled: Boolean = true,
        @Setting("start-height")
        var startHeight: Double = 2.073,
        val layouts: MutableList<HologramLayout> = mutableListOf(),
    ) {
        fun getLayout(joinState: String?): HologramLayout? {
            val normalizedState = joinState?.lowercase() ?: DEFAULT_JOIN_STATE
            return layouts.firstOrNull { it.joinState.equals(normalizedState, true) }
                ?: layouts.firstOrNull { it.joinState.equals(DEFAULT_JOIN_STATE, true) }
        }
    }

    @ConfigSerializable
    data class HologramLayout(
        @Setting("join-state")
        var joinState: String = DEFAULT_JOIN_STATE,
        val lines: MutableList<HologramConfiguration> = mutableListOf(),
    )

    @ConfigSerializable
    data class PushbackConfiguration(
        var enabled: Boolean = false,
        var radius: Double = 1.2,
        var strength: Double = 0.8,
        var vertical: Double = 0.3,
        var sound: String? = null,
    )

    @ConfigSerializable
    data class ActionConfiguration(
        @Setting("interaction-type")
        var interactionType: PlayerInteraction = PlayerInteraction.RIGHT_CLICK,
        @Setting("join-state")
        var joinState: String = DEFAULT_JOIN_STATE,
        @Setting("join-target")
        var joinTarget: Boolean = false,
        @Setting("open-inventory")
        var openInventory: String? = null,
        @Setting("play-sound")
        var playSound: String? = null,
        @Setting("execute-command")
        var executeCommand: String? = null,
        @Setting("send-message")
        var sendMessage: String? = null,
        var teleport: TeleportConfiguration? = null,
        @Setting("send-title")
        var sendTitle: TitleConfiguration? = null,
        @Setting("send-to-server")
        var sendToServer: String? = null,
        @Setting("transfer-to-server")
        var transferToServer: String? = null,
    )

    @ConfigSerializable
    data class TeleportConfiguration(
        val world: String = "world",
        val x: Double = 0.0,
        val y: Double = 0.0,
        val z: Double = 0.0,
        val yaw: Float = 0F,
        val pitch: Float = 0F,
    )

    @ConfigSerializable
    data class TitleConfiguration(
        val title: String = "",
        val subtitle: String = "",
        @Setting("fade-in")
        val fadeIn: Int = 20,
        val stay: Int = 20,
        @Setting("fade-out")
        val fadeOut: Int = 20,
    )

    fun actionsFor(interaction: PlayerInteraction, joinState: String?): List<ActionConfiguration> {
        val normalizedState = joinState?.lowercase() ?: DEFAULT_JOIN_STATE
        return actions.filter {
            it.interactionType == interaction && it.joinState.equals(normalizedState, true)
        }
    }

    fun normalize(): NpcConfig {
        hologram.layouts.forEach { it.joinState = it.joinState.lowercase() }
        actions.forEach { it.joinState = it.joinState.lowercase() }
        return this
    }

    /*
     * Kept as a source-compatible bridge for the old handler classes while the
     * version-4 option based command surface is retired.
     */
    @Deprecated("Version-4 compatibility type")
    @ConfigSerializable
    data class NpcInteraction(
        val playerInteraction: PlayerInteraction = PlayerInteraction.LEFT_CLICK,
        var action: Action = Action.RUN_COMMAND,
        val options: HashMap<String, String> = hashMapOf(),
    ) {
        fun getOption(key: String): Option? = options[key]?.let { Option(key, it) }
        fun getOptions(): List<Option> = options.map { Option(it.key, it.value) }
    }

    companion object {
        const val DEFAULT_JOIN_STATE = "default"
    }
}
