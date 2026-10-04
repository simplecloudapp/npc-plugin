package app.simplecloud.npc.core.config.migration

import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.config.migration.legacy.LegacyAction
import app.simplecloud.npc.core.config.migration.legacy.LegacyActionOptions
import app.simplecloud.npc.core.config.migration.legacy.LegacyNpcConfigV1
import app.simplecloud.npc.core.config.migration.legacy.LegacyNpcConfigV4
import app.simplecloud.npc.core.config.migration.legacy.LegacyNpcConfigV5
import app.simplecloud.npc.core.config.migration.legacy.LegacyPlayerActionOptions
import app.simplecloud.npc.core.config.migration.legacy.LegacyProviderType
import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.plugin.api.shared.config.ConfigMigration
import app.simplecloud.plugin.api.shared.config.ConfigMigrator
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import java.nio.file.Path

object NpcConfigMigration {
    private const val LEGACY_DEFAULT_JOIN_STATE = "default"
    private const val FALLBACK_VERSION = 4
    private val CURRENT_LAYOUT_KEYS = listOf("target-servers", "entity")

    fun migrator(): ConfigMigrator = ConfigMigrator.builder(NpcConfig.CURRENT_VERSION)
        .fallbackVersion(FALLBACK_VERSION)
        .migrate(1, NpcConfig.CURRENT_VERSION, ConfigMigration(::migrateVersionOne))
        .migrate(2, NpcConfig.CURRENT_VERSION, ConfigMigration(::migrateLegacy))
        .migrate(3, NpcConfig.CURRENT_VERSION, ConfigMigration(::migrateLegacy))
        .migrate(4, NpcConfig.CURRENT_VERSION, ConfigMigration(::migrateLegacy))
        .migrate(5, NpcConfig.CURRENT_VERSION, ConfigMigration(::migrateVersionFive))
        .build()

    fun stampCurrentLayout(node: ConfigurationNode) {
        val version = node.node("version")
        if (!version.virtual()) return
        if (CURRENT_LAYOUT_KEYS.any { !node.node(it).virtual() }) version.set(NpcConfig.CURRENT_VERSION)
    }

    fun backupOutdated(directory: Path) =
        ConfigBackups.backupOutdated(directory, NpcConfig.CURRENT_VERSION, FALLBACK_VERSION)

    private fun migrateVersionFive(node: CommentedConfigurationNode) {
        val legacy = node.get(LegacyNpcConfigV5::class.java) ?: return
        legacy.hologram.layouts.forEach { it.joinState = migrateJoinState(it.joinState) }
        legacy.actions.forEach { it.joinState = migrateJoinState(it.joinState) }

        val linked = legacy.provider.ownership.equals("LINKED", true) ||
            legacy.provider.type == LegacyProviderType.MYTHIC_MOBS
        val migrated = NpcConfig(
            id = legacy.id,
            entity = NpcConfig.NpcEntityConfiguration(
                provider = legacy.provider.type.commandName,
                providerReference = legacy.provider.reference.takeIf(String::isNotBlank),
                providerLinked = linked,
                needsRelocation = !linked,
            ),
            targetServers = legacy.targetServers,
            hologram = legacy.hologram,
            pushback = legacy.pushback,
            actions = legacy.actions,
        )

        replaceWith(node, migrated)
    }

    private fun migrateLegacy(node: CommentedConfigurationNode) {
        val legacy = node.get(LegacyNpcConfigV4::class.java) ?: return

        val target = sequenceOf(
            legacy.hologramConfiguration.placeholderServerBaseName,
            legacy.hologramConfiguration.placeholderName,
            legacy.hologramConfiguration.placeholderGroupName,
            legacy.options[LegacyActionOptions.GROUP_NAME],
            legacy.options[LegacyActionOptions.CONNECT_TO_SERVER_NAME],
        ).filterNotNull().firstOrNull(String::isNotBlank)

        val layouts = legacy.hologramConfiguration.holograms.map { old ->
            NpcConfig.HologramLayout(
                joinState = migrateJoinState(old.joinState),
                lines = old.lores.toMutableList(),
            )
        }.toMutableList()

        val actions = legacy.actions.map { old ->
            val options = legacy.options + old.options
            val oldAction = old.action.uppercase()
            if (oldAction == LegacyAction.RUN_CONSOLE_COMMAND.name) {
                NpcLog.logger.warning(
                    "NPC '${legacy.id}': the console command '${options[LegacyActionOptions.EXECUTE_COMMAND_NAME]}' " +
                        "now runs as the clicking player, console commands are no longer supported. " +
                        "Check that players may run it, or remove the action.",
                )
            }

            NpcConfig.ActionConfiguration(
                interactionType = old.playerInteraction,
                joinState = NpcConfig.DEFAULT_JOIN_STATE,
                joinTarget = oldAction == LegacyAction.QUICK_JOIN.name,
                openInventory = if (oldAction == LegacyAction.OPEN_INVENTORY.name) {
                    options[LegacyActionOptions.INVENTORY_NAME]?.takeIf(String::isNotBlank)
                } else null,
                playSound = options[LegacyPlayerActionOptions.PLAY_SOUND]?.takeIf(String::isNotBlank),
                executeCommand = when (oldAction) {
                    LegacyAction.RUN_COMMAND.name, LegacyAction.RUN_CONSOLE_COMMAND.name ->
                        options[LegacyActionOptions.EXECUTE_COMMAND_NAME]

                    else -> null
                }?.takeIf(String::isNotBlank),
                sendMessage = options[LegacyPlayerActionOptions.SEND_MESSAGE]?.takeIf(String::isNotBlank),
                sendTitle = options[LegacyPlayerActionOptions.SEND_TITLE]?.takeIf(String::isNotBlank)
                    ?.let { title ->
                        NpcConfig.TitleConfiguration(title, options[LegacyPlayerActionOptions.SEND_SUBTITLE].orEmpty())
                    },
                sendToServer = if (oldAction == LegacyAction.CONNECT_TO_SERVER.name) {
                    options[LegacyActionOptions.CONNECT_TO_SERVER_NAME]?.takeIf(String::isNotBlank)
                } else null,
                transferToServer = if (oldAction == LegacyAction.TRANSFER_TO_SERVER.name) {
                    options[LegacyActionOptions.TRANSFER_SERVER_IP]?.takeIf(String::isNotBlank)?.let { ip ->
                        val port = options[LegacyActionOptions.TRANSFER_SERVER_PORT]?.takeIf(String::isNotBlank)
                            ?: LegacyActionOptions.TRANSFER_SERVER_DEFAULT_PORT
                        "$ip:$port"
                    }
                } else null,
            )
        }.toMutableList()

        val migrated = NpcConfig(
            id = legacy.id,
            entity = NpcConfig.NpcEntityConfiguration(needsRelocation = true),
            targetServers = listOfNotNull(target).toMutableList(),
            hologram = NpcConfig.HologramConfigurationRoot(
                enabled = layouts.isNotEmpty(),
                startHeight = legacy.hologramConfiguration.holograms.firstOrNull()?.startHeight ?: 2.073,
                layouts = layouts,
            ),
            actions = actions,
        )

        replaceWith(node, migrated)
    }

    private fun migrateVersionOne(node: CommentedConfigurationNode) {
        val old = node.get(LegacyNpcConfigV1::class.java) ?: return
        val providerType = LegacyProviderType.getOrNull(old.provider) ?: LegacyProviderType.CITIZENS
        val migrated = NpcConfig(
            id = old.id,
            entity = NpcConfig.NpcEntityConfiguration(
                provider = providerType.commandName,
                providerReference = old.id,
                needsRelocation = true,
            ),
            targetServers = old.targetServers.toMutableList(),
            hologram = NpcConfig.HologramConfigurationRoot(
                old.hologram.enabled,
                old.hologram.startHeight,
                old.hologram.layouts.map {
                    NpcConfig.HologramLayout(
                        migrateJoinState(it.joinState),
                        mutableListOf(HologramLine(it.text)),
                    )
                }.toMutableList(),
            ),
            pushback = with(old.pushback) {
                NpcConfig.PushbackConfiguration(enabled, radius, strength, vertical, playSound)
            },
            actions = old.actions.map { action ->
                NpcConfig.ActionConfiguration(
                    interactionType = action.interactionType,
                    joinState = migrateJoinState(action.joinState),
                    openInventory = action.openInventory,
                    playSound = action.playSound,
                    executeCommand = action.executeCommand,
                    sendMessage = action.sendMessage,
                    teleport = action.teleport?.takeIf { it.enabled }?.let {
                        NpcLocation(it.world, it.x, it.y, it.z, it.yaw, it.pitch)
                    },
                    sendTitle = action.sendTitle?.takeIf { it.enabled }?.let {
                        NpcConfig.TitleConfiguration(it.title, it.text, it.fadeIn, it.stay, it.fadeOut)
                    },
                    sendToServer = action.sendToServer,
                    transferToServer = action.transferToServer,
                )
            }.toMutableList(),
        )

        replaceWith(node, migrated)
    }

    private fun migrateJoinState(joinState: String): String {
        val normalized = joinState.lowercase().ifBlank { NpcConfig.DEFAULT_JOIN_STATE }
        return if (normalized == LEGACY_DEFAULT_JOIN_STATE) NpcConfig.DEFAULT_JOIN_STATE else normalized
    }

    private fun replaceWith(node: CommentedConfigurationNode, migrated: NpcConfig) {
        node.childrenMap().keys.toList().forEach(node::removeChild)
        node.set(NpcConfig::class.java, migrated)
    }
}
