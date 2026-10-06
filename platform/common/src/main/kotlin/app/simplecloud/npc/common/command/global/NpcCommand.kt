package app.simplecloud.npc.common.command.global

import app.simplecloud.npc.common.command.AbstractNpcCommand
import app.simplecloud.npc.common.command.COMMAND_LABEL
import app.simplecloud.npc.common.command.COMMAND_PERMISSION
import app.simplecloud.npc.common.command.COMMAND_SYNTAX
import app.simplecloud.npc.common.command.CommandMessages
import app.simplecloud.npc.common.command.NpcInfoPage
import app.simplecloud.npc.common.command.NpcSuccessMessage
import app.simplecloud.npc.common.command.PlaceholderPage
import app.simplecloud.npc.common.manager.NpcOperationResult
import app.simplecloud.npc.common.manager.NpcRayTrace
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.common.text.sendError
import app.simplecloud.npc.common.text.sendInfo
import app.simplecloud.npc.common.text.sendWarning
import app.simplecloud.npc.common.utils.BackgroundTasks
import app.simplecloud.npc.common.utils.InputChecks
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.cloud.CloudListCache
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcCommandSender
import app.simplecloud.npc.core.platform.NpcPlayer
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import java.util.logging.Level

class NpcCommand<C : NpcCommandSender>(
    pluginContext: NpcPluginContext,
) : AbstractNpcCommand<C>(pluginContext) {

    @Command(COMMAND_SYNTAX)
    @Permission(COMMAND_PERMISSION)
    fun root(sender: C) = CommandMessages.sendHelp(sender)

    @Command("$COMMAND_SYNTAX help")
    @Permission(COMMAND_PERMISSION)
    fun help(sender: C) = CommandMessages.sendHelp(sender)

    @Command("$COMMAND_SYNTAX help <topic>")
    @Permission(COMMAND_PERMISSION)
    fun helpTopic(
        sender: C,
        @Argument("topic", suggestions = "helpTopics") topic: String,
    ) = CommandMessages.sendHelp(sender, topic)

    @Command("$COMMAND_SYNTAX placeholders [topic]")
    @Permission(COMMAND_PERMISSION)
    fun placeholders(
        sender: C,
        @Argument("topic", suggestions = "placeholderTopics") topic: String?,
    ) {
        PlaceholderPage.render(topic)?.let { sender.sendMessage(Msg.miniMessage.deserialize(it)) }
            ?: sender.sendError("Unknown category. Try: {}", PlaceholderPage.topics.joinToString())
    }

    @Command("$COMMAND_SYNTAX edit")
    @Permission(COMMAND_PERMISSION)
    fun editLookedAt(sender: C) {
        val (player, config) = lookedAt(sender, "edit") ?: return

        pluginContext.editors.openNpc(player, config)
    }

    @Command("$COMMAND_SYNTAX edit <id>")
    @Permission(COMMAND_PERMISSION)
    fun edit(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return
        val player = sender.asPlayer() ?: run {
            sender.sendInfo("Edit {}: target, hologram, action, pushback, teleport, skin", config.id)
            return
        }

        pluginContext.editors.openNpc(player, config)
    }

    @Command("$COMMAND_SYNTAX create <id> <target>")
    @Permission(COMMAND_PERMISSION)
    fun create(
        sender: C,
        @Argument("id") id: String,
        @Argument("target", suggestions = "targets") target: String,
    ) {
        val player = requirePlayer(sender) ?: return
        BackgroundTasks.launch {
            val result = pluginContext.npcManager.create(id, target, player)
            CommandMessages.sendResult(sender, result, NpcSuccessMessage.CREATED)

            if (result is NpcOperationResult.Success) {
                pluginContext.runSync { pluginContext.editors.openNpc(player, result.config) }
            }
        }
    }

    @Command("$COMMAND_SYNTAX link <id> <provider> <reference> <target>")
    @Permission(COMMAND_PERMISSION)
    fun link(
        sender: C,
        @Argument("id") id: String,
        @Argument("provider", suggestions = "linkProviders") provider: String,
        @Argument("reference", suggestions = "linkReferences") reference: String,
        @Argument("target", suggestions = "targets") target: String,
    ) {
        BackgroundTasks.launch {
            val result = pluginContext.npcManager.link(id, target, provider, reference)
            CommandMessages.sendResult(sender, result, NpcSuccessMessage.LINKED)
        }
    }

    @Command("$COMMAND_SYNTAX duplicate <id> <newId>")
    @Permission(COMMAND_PERMISSION)
    fun duplicate(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("newId") newId: String,
    ) {
        val config = findNpc(sender, id) ?: return
        val player = requirePlayer(sender) ?: return

        CommandMessages.sendResult(
            sender,
            pluginContext.npcManager.duplicate(config.id, newId, player.location()),
            NpcSuccessMessage.DUPLICATED,
        )
    }

    @Command("$COMMAND_SYNTAX unlink <id>")
    @Permission(COMMAND_PERMISSION)
    fun unlink(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return

        CommandMessages.sendResult(sender, pluginContext.npcManager.unlink(config.id), NpcSuccessMessage.UNLINKED)
    }

    @Command("$COMMAND_SYNTAX delete")
    @Permission(COMMAND_PERMISSION)
    fun deleteLookedAt(sender: C) {
        val (_, config) = lookedAt(sender, "delete") ?: return

        CommandMessages.sendResult(sender, pluginContext.npcManager.delete(config.id), NpcSuccessMessage.DELETED)
    }

    @Command("$COMMAND_SYNTAX delete <id>")
    @Permission(COMMAND_PERMISSION)
    fun delete(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return

        CommandMessages.sendResult(sender, pluginContext.npcManager.delete(config.id), NpcSuccessMessage.DELETED)
    }

    @Command("$COMMAND_SYNTAX edit <id> teleport")
    @Permission(COMMAND_PERMISSION)
    fun teleport(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return
        val player = requirePlayer(sender) ?: return

        CommandMessages.sendResult(sender, pluginContext.npcManager.teleport(config.id, player.location()))
    }

    @Command("$COMMAND_SYNTAX tp <id>")
    @Permission(COMMAND_PERMISSION)
    fun teleportTo(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return
        val player = requirePlayer(sender) ?: return
        if (config.entity.needsRelocation) {
            sender.sendError("NPC {} has not been placed yet.", config.id)
            return
        }
        val location = config.entity.location
        if (!player.teleport(location.inFrontLookingBack(TELEPORT_DISTANCE))) {
            sender.sendError("The world {} of NPC {} is not loaded.", location.world, config.id)
        }
    }

    @Command("$COMMAND_SYNTAX edit <id> skin <player>")
    @Permission(COMMAND_PERMISSION)
    fun skin(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("player") input: String,
    ) {
        val config = findNpc(sender, id) ?: return
        pluginContext.playerDirectory.findOnlinePlayer(input)?.let { online ->
            val skin = pluginContext.renderer.captureSkin(online)
            CommandMessages.sendResult(sender, pluginContext.npcManager.updateSkin(config.id, skin))
            return
        }

        sender.sendInfo("Looking up the skin for {}...", input)
        BackgroundTasks.launch {
            val skin = pluginContext.npcManager.lookupSkin(input) ?: run {
                sender.sendError("Could not find a skin for {}.", input)
                return@launch
            }
            CommandMessages.sendResult(sender, pluginContext.npcManager.updateSkin(config.id, skin))
        }
    }

    @Command("$COMMAND_SYNTAX edit <id> skin data <data>")
    @Permission(COMMAND_PERMISSION)
    fun skinByData(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("data", parserName = "greedyString") data: String,
    ) {
        val config = findNpc(sender, id) ?: return
        val parts = data.trim().split(Regex("\\s+"))
        val texture = parts.getOrNull(0).orEmpty()
        val signature = parts.getOrNull(1).orEmpty()
        if (parts.size != 2 || !InputChecks.isValidBase64(texture) || !InputChecks.isValidBase64(signature)) {
            sender.sendError("The provided skin data is invalid.")
            return
        }
        val skin = NpcConfig.SkinConfiguration(texture, signature)
        CommandMessages.sendResult(sender, pluginContext.npcManager.updateSkin(config.id, skin))
    }

    @Command("$COMMAND_SYNTAX list")
    @Permission(COMMAND_PERMISSION)
    fun list(sender: C) {
        val configs = pluginContext.npcRepository.findAll().ifEmpty {
            sender.sendWarning("No NPCs were found.")
            return
        }
        val lines = buildList {
            add("${Msg.infoPrefix()}Configured NPCs ${Msg.SUBTLE}(${configs.size})")
            configs.sortedBy { it.id }.forEach {
                add("   ${Msg.SUBTLE}- ${Msg.ACCENT}${it.id} ${Msg.SUBTLE}(${it.targetServers.joinToString()})")
            }
        }
        sender.sendMessage(Msg.miniMessage.deserialize(lines.joinToString("\n")))
    }

    @Command("$COMMAND_SYNTAX info")
    @Permission(COMMAND_PERMISSION)
    fun infoLookedAt(sender: C) {
        val (_, config) = lookedAt(sender, "info") ?: return

        sendInfo(sender, config)
    }

    @Command("$COMMAND_SYNTAX info <id>")
    @Permission(COMMAND_PERMISSION)
    fun info(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return

        sendInfo(sender, config)
    }

    private fun sendInfo(sender: C, config: NpcConfig) {
        val snapshot = CloudListCache.peek()
        val status = pluginContext.npcManager.statusOf(config, snapshot)
        sender.sendMessage(Msg.miniMessage.deserialize(NpcInfoPage.render(config, status, snapshot)))
    }

    @Command("$COMMAND_SYNTAX reload")
    @Permission(COMMAND_PERMISSION)
    fun reload(sender: C) {
        val result = runCatching {
            pluginContext.npcManager.reload()
            pluginContext.inventoryRepository.reload()
            pluginContext.inventoryViews.requestRefresh()
        }.onFailure { NpcLog.logger.log(Level.WARNING, "Reload failed", it) }
        if (!CommandMessages.reload()) {
            sender.sendWarning("messages.yml could not be read, the previous messages stay in use. See the console.")
        }
        val (path, fallback) = if (result.isSuccess) {
            "command.reload.success" to Msg.success("SimpleCloud NPCs was reloaded.")
        } else {
            "command.reload.failed" to Msg.error("SimpleCloud NPCs could not be reloaded.")
        }
        CommandMessages.send(sender, path, fallback)
    }

    private fun lookedAt(sender: C, subcommand: String): Pair<NpcPlayer, NpcConfig>? {
        val player = sender.asPlayer() ?: run {
            sender.sendError("Specify an id from the console: /{} {} <id>.", COMMAND_LABEL, subcommand)
            return null
        }

        val config = NpcRayTrace.findLookedAt(pluginContext.npcRepository, player) ?: run {
            player.sendError("You're not looking at an NPC, get closer or use /{} {} <id>.", COMMAND_LABEL, subcommand)
            return null
        }

        return player to config
    }

    private companion object {
        const val TELEPORT_DISTANCE = 2.0
    }
}
