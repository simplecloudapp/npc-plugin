package app.simplecloud.npc.shared.config

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.ActionOptions
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.hologram.config.HologramConfiguration
import app.simplecloud.npc.shared.player.PlayerActionOptions
import app.simplecloud.npc.shared.provider.NpcProviderRegistry
import app.simplecloud.npc.shared.provider.NpcProviderType
import app.simplecloud.npc.shared.provider.ProviderOwnership
import app.simplecloud.npc.shared.utils.ConfigVersion
import app.simplecloud.plugin.api.shared.config.ConfigMigration
import app.simplecloud.plugin.api.shared.config.ConfigMigrator
import org.bukkit.Bukkit
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Files
import java.nio.file.Path

object NpcConfigMigration {
    fun migrator(providerRegistry: NpcProviderRegistry): ConfigMigrator {
        return ConfigMigrator.builder(ConfigVersion.VERSION)
            .fallbackVersion(4)
            .migrate(1, ConfigVersion.VERSION, ConfigMigration { node -> migrateProposedVersionOne(node, providerRegistry) })
            .migrate(2, ConfigVersion.VERSION, ConfigMigration { node -> migrateLegacy(node, providerRegistry) })
            .migrate(3, ConfigVersion.VERSION, ConfigMigration { node -> migrateLegacy(node, providerRegistry) })
            .migrate(4, ConfigVersion.VERSION, ConfigMigration { node -> migrateVersionFour(node, providerRegistry) })
            .build()
    }

    fun backupOutdated(directory: Path) {
        if (!Files.exists(directory)) return
        Files.createDirectories(directory.resolve("backups"))
        Files.list(directory).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.isYaml() }.forEach { path ->
                val version = runCatching {
                    YamlConfigurationLoader.builder().path(path).build().load().node("version").getInt(4)
                }.getOrDefault(4)
                if (version >= ConfigVersion.VERSION) return@forEach
                val backup = directory.resolve("backups").resolve("${path.fileName}.v$version.bak")
                if (!Files.exists(backup)) Files.copy(path, backup)
            }
        }
    }

    private fun migrateVersionFour(node: CommentedConfigurationNode, registry: NpcProviderRegistry) {
        migrateLegacy(node, registry)
    }

    private fun migrateLegacy(node: CommentedConfigurationNode, registry: NpcProviderRegistry) {
        val legacy = node.get(LegacyNpcConfig::class.java) ?: return
        val provider = registry.availableProviders(Bukkit.getPluginManager())
            .firstOrNull { runCatching { it.exists(legacy.id) }.getOrDefault(false) }
            ?: registry.availableProviders(Bukkit.getPluginManager()).firstOrNull()

        val target = sequenceOf(
            legacy.hologramConfiguration.placeholderServerBaseName,
            legacy.hologramConfiguration.placeholderName,
            legacy.hologramConfiguration.placeholderGroupName,
            legacy.options[ActionOptions.GROUP_NAME.first],
            legacy.options[ActionOptions.CONNECT_TO_SERVER_NAME.first],
        ).filterNotNull().firstOrNull(String::isNotBlank)

        val layouts = legacy.hologramConfiguration.holograms.map { old ->
            NpcConfig.HologramLayout(
                joinState = old.joinState.ifBlank { NpcConfig.DEFAULT_JOIN_STATE }.lowercase(),
                lines = old.lores.toMutableList(),
            )
        }.toMutableList()

        val actions = legacy.actions.map { old ->
            val options = legacy.options + old.options
            val oldAction = old.action.uppercase()
            NpcConfig.ActionConfiguration(
                interactionType = old.playerInteraction,
                joinState = NpcConfig.DEFAULT_JOIN_STATE,
                joinTarget = oldAction == Action.QUICK_JOIN.name,
                openInventory = if (oldAction == "OPEN_INVENTORY") {
                    options["inventory.name"]?.takeIf(String::isNotBlank)
                } else null,
                playSound = options[PlayerActionOptions.PLAY_SOUND.first]?.takeIf(String::isNotBlank),
                executeCommand = when (oldAction) {
                    Action.RUN_COMMAND.name, Action.RUN_CONSOLE_COMMAND.name -> options[ActionOptions.EXECUTE_COMMAND_NAME.first]
                    else -> null
                }?.takeIf(String::isNotBlank),
                sendMessage = options[PlayerActionOptions.SEND_MESSAGE.first]?.takeIf(String::isNotBlank),
                sendTitle = options[PlayerActionOptions.SEND_TITLE.first]?.takeIf(String::isNotBlank)?.let { title ->
                    NpcConfig.TitleConfiguration(title, options[PlayerActionOptions.SEND_SUBTITLE.first].orEmpty())
                },
                sendToServer = if (oldAction == Action.CONNECT_TO_SERVER.name) {
                    options[ActionOptions.CONNECT_TO_SERVER_NAME.first]?.takeIf(String::isNotBlank)
                } else null,
                transferToServer = if (oldAction == Action.TRANSFER_TO_SERVER.name) {
                    options[ActionOptions.TRANSFER_SERVER_IP.first]?.takeIf(String::isNotBlank)?.let { ip ->
                        "$ip:${options[ActionOptions.TRANSFER_SERVER_PORT.first] ?: "25565"}"
                    }
                } else null,
            )
        }.toMutableList()

        val migrated = NpcConfig(
            id = legacy.id,
            provider = NpcConfig.ProviderConfiguration(
                type = provider?.type ?: NpcProviderType.CITIZENS,
                reference = legacy.id,
                ownership = ProviderOwnership.LINKED,
            ),
            targetServers = target?.let { mutableListOf(it) } ?: mutableListOf(),
            hologram = NpcConfig.HologramConfigurationRoot(
                enabled = layouts.isNotEmpty(),
                startHeight = legacy.hologramConfiguration.holograms.firstOrNull()?.startHeight ?: 2.073,
                layouts = layouts,
            ),
            actions = actions,
        )
        node.set(NpcConfig::class.java, migrated)
    }

    private fun migrateProposedVersionOne(node: CommentedConfigurationNode, registry: NpcProviderRegistry) {
        val old = node.get(ProposedNpcConfig::class.java) ?: return
        val type = NpcProviderType.getOrNull(old.provider)
            ?: registry.availableProviders(Bukkit.getPluginManager()).firstOrNull()?.type
            ?: NpcProviderType.CITIZENS
        val migrated = NpcConfig(
            id = old.id,
            provider = NpcConfig.ProviderConfiguration(type, old.id, ProviderOwnership.LINKED),
            targetServers = old.targetServers.toMutableList(),
            hologram = NpcConfig.HologramConfigurationRoot(
                old.hologram.enabled,
                old.hologram.startHeight,
                old.hologram.layouts.map {
                    NpcConfig.HologramLayout(
                        it.joinState.ifBlank { NpcConfig.DEFAULT_JOIN_STATE }.lowercase(),
                        mutableListOf(HologramConfiguration(it.text)),
                    )
                }.toMutableList(),
            ),
            pushback = NpcConfig.PushbackConfiguration(
                old.pushback.enabled,
                old.pushback.radius,
                old.pushback.strength,
                old.pushback.vertical,
                old.pushback.playSound,
            ),
            actions = old.actions.map { action ->
                NpcConfig.ActionConfiguration(
                    interactionType = action.interactionType,
                    joinState = action.joinState.ifBlank { NpcConfig.DEFAULT_JOIN_STATE }.lowercase(),
                    openInventory = action.openInventory,
                    playSound = action.playSound,
                    executeCommand = action.executeCommand,
                    sendMessage = action.sendMessage,
                    teleport = action.teleport?.takeIf { it.enabled }?.let {
                        NpcConfig.TeleportConfiguration(it.world, it.x, it.y, it.z, it.yaw, it.pitch)
                    },
                    sendTitle = action.sendTitle?.takeIf { it.enabled }?.let {
                        NpcConfig.TitleConfiguration(it.title, it.text, it.fadeIn, it.stay, it.fadeOut)
                    },
                    sendToServer = action.sendToServer,
                    transferToServer = action.transferToServer,
                )
            }.toMutableList(),
        )
        node.set(NpcConfig::class.java, migrated)
    }

    private fun Path.isYaml(): Boolean {
        val name = fileName.toString().lowercase()
        return name.endsWith(".yml") || name.endsWith(".yaml")
    }

    @ConfigSerializable
    data class LegacyNpcConfig(
        val version: String = "4",
        val id: String = "",
        val hologramConfiguration: LegacyHologramRoot = LegacyHologramRoot(),
        val actions: MutableList<LegacyInteraction> = mutableListOf(),
        val options: HashMap<String, String> = hashMapOf(),
    )

    @ConfigSerializable
    data class LegacyInteraction(
        val playerInteraction: PlayerInteraction = PlayerInteraction.LEFT_CLICK,
        val action: String = Action.RUN_COMMAND.name,
        val options: HashMap<String, String> = hashMapOf(),
    )

    @ConfigSerializable
    data class LegacyHologramRoot(
        val placeholderServerBaseName: String = "",
        val placeholderName: String = "",
        val placeholderGroupName: String = "",
        val holograms: List<LegacyHologram> = emptyList(),
    )

    @ConfigSerializable
    data class LegacyHologram(
        val startHeight: Double = 2.073,
        val joinState: String = "",
        val lores: List<HologramConfiguration> = emptyList(),
    )

    @ConfigSerializable
    data class ProposedNpcConfig(
        val version: String = "1",
        val id: String = "",
        val provider: String = "CITIZENS",
        @org.spongepowered.configurate.objectmapping.meta.Setting("target-servers")
        val targetServers: List<String> = emptyList(),
        val hologram: ProposedHologram = ProposedHologram(),
        val pushback: ProposedPushback = ProposedPushback(),
        val actions: List<ProposedAction> = emptyList(),
    )

    @ConfigSerializable
    data class ProposedHologram(
        val enabled: Boolean = true,
        @org.spongepowered.configurate.objectmapping.meta.Setting("start-height")
        val startHeight: Double = 2.073,
        val layouts: List<ProposedLayout> = emptyList(),
    )

    @ConfigSerializable
    data class ProposedLayout(
        val joinstate: String = NpcConfig.DEFAULT_JOIN_STATE,
        val text: String = "",
    ) {
        val joinState: String get() = joinstate
    }

    @ConfigSerializable
    data class ProposedPushback(
        val enabled: Boolean = false,
        val radius: Double = 2.5,
        val strength: Double = 1.2,
        val vertical: Double = 0.3,
        @org.spongepowered.configurate.objectmapping.meta.Setting("playSound")
        val playSound: String? = null,
    )

    @ConfigSerializable
    data class ProposedAction(
        @org.spongepowered.configurate.objectmapping.meta.Setting("interaction-type")
        val interactionType: PlayerInteraction = PlayerInteraction.RIGHT_CLICK,
        val joinstate: String = NpcConfig.DEFAULT_JOIN_STATE,
        @org.spongepowered.configurate.objectmapping.meta.Setting("openInventory")
        val openInventory: String? = null,
        @org.spongepowered.configurate.objectmapping.meta.Setting("playSound")
        val playSound: String? = null,
        @org.spongepowered.configurate.objectmapping.meta.Setting("executeCommand")
        val executeCommand: String? = null,
        @org.spongepowered.configurate.objectmapping.meta.Setting("sendMessage")
        val sendMessage: String? = null,
        val teleport: ProposedTeleport? = null,
        @org.spongepowered.configurate.objectmapping.meta.Setting("sendTitle")
        val sendTitle: ProposedTitle? = null,
        @org.spongepowered.configurate.objectmapping.meta.Setting("sendToServer")
        val sendToServer: String? = null,
        @org.spongepowered.configurate.objectmapping.meta.Setting("transferToServer")
        val transferToServer: String? = null,
    ) {
        val joinState: String get() = joinstate
    }

    @ConfigSerializable
    data class ProposedTeleport(
        val enabled: Boolean = false,
        val world: String = "world",
        val x: Double = 0.0,
        val y: Double = 0.0,
        val z: Double = 0.0,
        val yaw: Float = 0F,
        val pitch: Float = 0F,
    )

    @ConfigSerializable
    data class ProposedTitle(
        val enabled: Boolean = false,
        val title: String = "",
        val text: String = "",
        @org.spongepowered.configurate.objectmapping.meta.Setting("fadeIn")
        val fadeIn: Int = 20,
        val stay: Int = 20,
        @org.spongepowered.configurate.objectmapping.meta.Setting("fadeOut")
        val fadeOut: Int = 20,
    )
}
