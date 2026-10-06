package app.simplecloud.npc.core.config

import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.core.render.Providers
import app.simplecloud.plugin.api.shared.config.VersionedConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class NpcConfig(
    override val version: Int = CURRENT_VERSION,
    val id: String = "",
    val entity: NpcEntityConfiguration = NpcEntityConfiguration(),
    @Setting("target-servers")
    val targetServers: MutableList<String> = mutableListOf(),
    @Setting("join-strategy")
    val joinStrategy: JoinStrategy = JoinStrategy.LEAST_PLAYERS,
    val hologram: HologramConfigurationRoot = HologramConfigurationRoot(),
    val pushback: PushbackConfiguration = PushbackConfiguration(),
    val actions: MutableList<ActionConfiguration> = mutableListOf(),
    @Setting("join-states")
    val joinStates: MutableList<String> = mutableListOf(),
) : VersionedConfig {

    @ConfigSerializable
    data class NpcEntityConfiguration(
        val location: NpcLocation = NpcLocation(),
        val skin: SkinConfiguration = SkinConfiguration(),
        @Setting("look-at-player")
        val lookAtPlayer: Boolean = true,
        @Setting("look-at-player-distance")
        val lookAtPlayerDistance: Double = 10.0,
        @Setting("view-distance")
        val viewDistance: Double = 48.0,
        val glowing: Boolean = false,
        @Setting("glow-color")
        val glowColor: String? = null,
        @Setting("custom-name")
        val customName: String? = null,
        val equipment: EquipmentConfiguration = EquipmentConfiguration(),
        val pose: NpcPose = NpcPose.STANDING,
        val scale: Double = 1.0,
        val provider: String = Providers.STANDALONE,
        @Setting("provider-reference")
        val providerReference: String? = null,
        @Setting("provider-linked")
        val providerLinked: Boolean = false,
        @Setting("needs-relocation")
        val needsRelocation: Boolean = false,
        @Setting("dormant-provider-references")
        val dormantProviderReferences: Map<String, String> = emptyMap(),
    ) {
        fun effectiveScale(): Double = scale.coerceIn(SCALE_RANGE)

        fun heightFactor(): Double = effectiveScale() * pose.heightFactor

        companion object {
            val LOOK_AT_PLAYER_DISTANCE_RANGE = 0.5..32.0
            val VIEW_DISTANCE_RANGE = 16.0..128.0
            val SCALE_RANGE = 0.25..4.0
        }
    }

    @ConfigSerializable
    data class EquipmentConfiguration(
        @Setting("main-hand")
        val mainHand: EquipmentItem? = null,
        @Setting("off-hand")
        val offHand: EquipmentItem? = null,
        val helmet: EquipmentItem? = null,
        val chestplate: EquipmentItem? = null,
        val leggings: EquipmentItem? = null,
        val boots: EquipmentItem? = null,
    ) {
        fun of(slot: EquipmentSlot): EquipmentItem? = when (slot) {
            EquipmentSlot.MAIN_HAND -> mainHand
            EquipmentSlot.OFF_HAND -> offHand
            EquipmentSlot.HELMET -> helmet
            EquipmentSlot.CHESTPLATE -> chestplate
            EquipmentSlot.LEGGINGS -> leggings
            EquipmentSlot.BOOTS -> boots
        }

        fun with(slot: EquipmentSlot, item: EquipmentItem?): EquipmentConfiguration = when (slot) {
            EquipmentSlot.MAIN_HAND -> copy(mainHand = item)
            EquipmentSlot.OFF_HAND -> copy(offHand = item)
            EquipmentSlot.HELMET -> copy(helmet = item)
            EquipmentSlot.CHESTPLATE -> copy(chestplate = item)
            EquipmentSlot.LEGGINGS -> copy(leggings = item)
            EquipmentSlot.BOOTS -> copy(boots = item)
        }

        fun filled(): Map<EquipmentSlot, EquipmentItem> =
            EquipmentSlot.entries.mapNotNull { slot -> of(slot)?.let { slot to it } }.toMap()
    }

    @ConfigSerializable
    data class EquipmentItem(
        val material: String = "STONE",
        val glowing: Boolean = false,
        @Setting("head-texture")
        val headTexture: String? = null,
        val color: String? = null,
        val trim: ArmorTrim? = null,
        @Setting("custom-model-data")
        val customModelData: Int? = null,
    )

    @ConfigSerializable
    data class ArmorTrim(
        val material: String = "minecraft:iron",
        val pattern: String = "minecraft:coast",
    )

    @ConfigSerializable
    data class SkinConfiguration(
        val texture: String? = null,
        val signature: String? = null,
        @Setting("source-player")
        val sourcePlayer: String? = null,
    )

    @ConfigSerializable
    data class HologramConfigurationRoot(
        var enabled: Boolean = true,
        @Setting("start-height")
        var startHeight: Double = 2.073,
        val layouts: MutableList<HologramLayout> = mutableListOf(),
    ) {
        fun layoutShownFor(joinState: String?): HologramLayout? {
            val normalizedState = joinState?.lowercase() ?: DEFAULT_JOIN_STATE
            return layouts.firstOrNull { it.joinState.equals(normalizedState, true) }
                ?: layouts.firstOrNull { it.joinState.equals(DEFAULT_JOIN_STATE, true) }
        }

        fun findLayout(joinState: String): HologramLayout? =
            layouts.firstOrNull { it.joinState.equals(joinState, true) }

        fun layoutOrCreate(joinState: String): HologramLayout {
            val state = joinState.lowercase()
            return findLayout(state) ?: HologramLayout(state).also(layouts::add)
        }

        companion object {
            val START_HEIGHT_RANGE = 0.0..10.0
        }
    }

    @ConfigSerializable
    data class HologramLayout(
        @Setting("join-state")
        var joinState: String = DEFAULT_JOIN_STATE,
        val lines: MutableList<HologramLine> = mutableListOf(),
    )

    @ConfigSerializable
    data class PushbackConfiguration(
        var enabled: Boolean = false,
        var radius: Double = 1.2,
        var strength: Double = 0.8,
        var vertical: Double = 0.3,
        var sound: String? = null,
        @Setting("sound-options")
        var soundOptions: SoundOptions = SoundOptions(),
    ) {
        companion object {
            val RADIUS_RANGE = 0.5..20.0
            val STRENGTH_RANGE = 0.0..10.0
            val VERTICAL_RANGE = 0.0..5.0
        }
    }

    @ConfigSerializable
    data class SoundOptions(
        val volume: Double = 1.0,
        val pitch: Double = 1.0,
    ) {
        companion object {
            val VOLUME_RANGE = 0.0..10.0
            val PITCH_RANGE = 0.5..2.0
        }
    }

    @ConfigSerializable
    data class ActionConfiguration(
        @Setting("interaction-type")
        val interactionType: PlayerInteraction = PlayerInteraction.RIGHT_CLICK,
        @Setting("join-state")
        var joinState: String = DEFAULT_JOIN_STATE,
        @Setting("join-target")
        var joinTarget: Boolean = false,
        @Setting("open-inventory")
        var openInventory: String? = null,
        @Setting("play-sound")
        var playSound: String? = null,
        @Setting("play-sound-options")
        var playSoundOptions: SoundOptions = SoundOptions(),
        @Setting("execute-command")
        var executeCommand: String? = null,
        @Setting("send-message")
        var sendMessage: String? = null,
        var teleport: NpcLocation? = null,
        @Setting("send-title")
        var sendTitle: TitleConfiguration? = null,
        @Setting("send-to-server")
        var sendToServer: String? = null,
        @Setting("transfer-to-server")
        var transferToServer: String? = null,
        @Setting("action-bar")
        var actionBar: String? = null,
        @Setting("close-menu")
        var closeMenu: Boolean = false,
        @Setting("previous-menu")
        var previousMenu: Boolean = false,
        @Setting("npc-animation")
        var npcAnimation: NpcAnimation? = null,
        var speech: String? = null,
        var permission: String? = null,
        @Setting("deny-message")
        var denyMessage: String? = null,
        var cooldown: Long = DEFAULT_COOLDOWN_MILLIS,
    ) {
        fun isEmpty(): Boolean = configuredTypes().isEmpty() &&
            permission == null &&
            denyMessage == null &&
            cooldown == DEFAULT_COOLDOWN_MILLIS

        fun configuredTypes(): List<String> = buildList {
            if (joinTarget) add(ActionFields.JOIN_TARGET)
            if (openInventory != null) add(ActionFields.OPEN_INVENTORY)
            if (playSound != null) add(ActionFields.PLAY_SOUND)
            if (executeCommand != null) add(ActionFields.EXECUTE_COMMAND)
            if (sendMessage != null) add(ActionFields.SEND_MESSAGE)
            if (teleport != null) add(ActionFields.TELEPORT)
            if (sendTitle != null) add(ActionFields.SEND_TITLE)
            if (sendToServer != null) add(ActionFields.SEND_TO_SERVER)
            if (transferToServer != null) add(ActionFields.TRANSFER_TO_SERVER)
            if (actionBar != null) add(ActionFields.ACTION_BAR)
            if (closeMenu) add(ActionFields.CLOSE_MENU)
            if (previousMenu) add(ActionFields.PREVIOUS_MENU)
            if (npcAnimation != null) add(ActionFields.NPC_ANIMATION)
            if (speech != null) add(ActionFields.SPEECH)
        }

        fun clearField(type: String): Boolean {
            when (type.lowercase()) {
                ActionFields.JOIN_TARGET -> joinTarget = false
                ActionFields.OPEN_INVENTORY -> openInventory = null
                ActionFields.PLAY_SOUND -> playSound = null
                ActionFields.EXECUTE_COMMAND -> executeCommand = null
                ActionFields.SEND_MESSAGE -> sendMessage = null
                ActionFields.TELEPORT -> teleport = null
                ActionFields.SEND_TITLE -> sendTitle = null
                ActionFields.SEND_TO_SERVER -> sendToServer = null
                ActionFields.TRANSFER_TO_SERVER -> transferToServer = null
                ActionFields.ACTION_BAR -> actionBar = null
                ActionFields.CLOSE_MENU -> closeMenu = false
                ActionFields.PREVIOUS_MENU -> previousMenu = false
                ActionFields.NPC_ANIMATION -> npcAnimation = null
                ActionFields.SPEECH -> speech = null
                else -> return false
            }

            return true
        }
    }

    @ConfigSerializable
    data class TitleConfiguration(
        val title: String = "",
        val subtitle: String = "",
        @Setting("fade-in")
        val fadeIn: Int = 20,
        val stay: Int = 20,
        @Setting("fade-out")
        val fadeOut: Int = 20,
    ) {
        companion object {
            val FADE_RANGE = 0..100
            val STAY_RANGE = 0..200
        }
    }

    fun actionsFor(interaction: PlayerInteraction, joinState: String?): List<ActionConfiguration> {
        val normalizedState = joinState?.lowercase() ?: DEFAULT_JOIN_STATE
        val configured = actions.filter { it.interactionType == interaction && !it.isEmpty() }
        val exact = configured.filter { it.joinState.equals(normalizedState, true) }
        if (exact.isNotEmpty() || normalizedState.equals(DEFAULT_JOIN_STATE, true)) return exact

        return configured.filter { it.joinState.equals(DEFAULT_JOIN_STATE, true) }
    }

    fun findAction(interaction: PlayerInteraction, joinState: String): ActionConfiguration? =
        actions.firstOrNull { it.interactionType == interaction && it.joinState.equals(joinState, true) }

    fun actionOrCreate(interaction: PlayerInteraction, joinState: String): ActionConfiguration =
        findAction(interaction, joinState)
            ?: ActionConfiguration(interactionType = interaction, joinState = joinState.lowercase()).also(actions::add)

    fun dropActionIfEmpty(interaction: PlayerInteraction, joinState: String) {
        val action = findAction(interaction, joinState) ?: return

        if (action.isEmpty()) actions.remove(action)
    }

    fun normalize(): NpcConfig = apply {
        hologram.layouts.forEach { it.joinState = it.joinState.lowercase() }
        actions.forEach { it.joinState = it.joinState.lowercase() }
        val normalizedJoinStates = joinStates.map { it.lowercase() }.distinct()
        joinStates.clear()
        joinStates.addAll(normalizedJoinStates)
    }

    fun deepCopy(): NpcConfig = copy(
        targetServers = targetServers.toMutableList(),
        hologram = hologram.copy(
            layouts = hologram.layouts.map {
                it.copy(lines = it.lines.toMutableList())
            }.toMutableList(),
        ),
        pushback = pushback.copy(),
        actions = actions.map { it.copy() }.toMutableList(),
        joinStates = joinStates.toMutableList(),
    )

    companion object {
        const val CURRENT_VERSION = 6
        const val DEFAULT_JOIN_STATE = "public"
        const val DEFAULT_COOLDOWN_MILLIS = 1_000L
        const val MAX_TARGETS = 8
        const val MAX_JOIN_STATES = 8
    }
}
