package app.simplecloud.npc.plugin.paper.command.edit

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.COMMAND_PERMISSION
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.plugin.api.shared.extension.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

class NpcActionCommand(namespace: NpcNamespace) : AbstractNpcCommand(namespace) {

    @Command("$commandName edit <id> action")
    @Permission(COMMAND_PERMISSION)
    fun info(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val visibleActions = config.actions.filter { it.configuredTypes().isNotEmpty() }
        if (visibleActions.isEmpty()) {
            sender.sender.sendMessage(text("$PREFIX <#f59e0b>NPC <#f8fafc>$id <#f59e0b>has no actions."))
            return
        }
        var message = text("$PREFIX <#0ea5e9>Actions for <#f8fafc>$id")
        visibleActions.forEach {
            message = message.appendNewline().append(
                text("   <#475569>- <#f8fafc>${it.interactionType.name.lowercase()} <#475569>(${it.joinState}) -> <#e2e8f0>${it.configuredTypes().joinToString()}")
            )
        }
        sender.sender.sendMessage(message)
    }

    @Command("$commandName edit <id> action join-target <interaction> <joinState> <enabled>")
    @Permission(COMMAND_PERMISSION)
    fun joinTarget(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interactionName: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("enabled") enabled: Boolean,
    ) = update(sender, id, interactionName, joinState, "join-target") { it.joinTarget = enabled }

    @Command("$commandName edit <id> action play-sound <interaction> <joinState> <sound>")
    @Permission(COMMAND_PERMISSION)
    fun playSound(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("sound", suggestions = "sounds") sound: String,
    ) {
        runCatching { Key.key(sound) }.getOrElse {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Sound <#f8fafc>$sound <#dc2626>is invalid."))
            return
        }
        update(sender, id, interaction, joinState, "play-sound") { it.playSound = sound }
    }

    @Command("$commandName edit <id> action execute-command <interaction> <joinState> <command>")
    @Permission(COMMAND_PERMISSION)
    fun executeCommand(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("command", parserName = "greedyString") command: String,
    ) = update(sender, id, interaction, joinState, "execute-command") {
        it.executeCommand = command.removePrefix("/")
    }

    @Command("$commandName edit <id> action send-message <interaction> <joinState> <message>")
    @Permission(COMMAND_PERMISSION)
    fun sendMessage(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("message", parserName = "greedyString") message: String,
    ) = update(sender, id, interaction, joinState, "send-message") { it.sendMessage = message }

    @Command("$commandName edit <id> action teleport-here <interaction> <joinState>")
    @Permission(COMMAND_PERMISSION)
    fun teleportHere(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
    ) {
        val player = sender.sender as? Player ?: run {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be used by a player."))
            return
        }
        val location = player.location
        update(sender, id, interaction, joinState, "teleport") {
            it.teleport = NpcConfig.TeleportConfiguration(
                location.world.name,
                location.x,
                location.y,
                location.z,
                location.yaw,
                location.pitch,
            )
        }
    }

    @Command("$commandName edit <id> action send-title <interaction> <joinState> <title> <subtitle>")
    @Permission(COMMAND_PERMISSION)
    fun sendTitle(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("title") title: String,
        @Argument("subtitle") subtitle: String,
    ) = update(sender, id, interaction, joinState, "send-title") {
        it.sendTitle = NpcConfig.TitleConfiguration(title, subtitle)
    }

    @Command("$commandName edit <id> action title <interaction> <joinState> <title>")
    @Permission(COMMAND_PERMISSION)
    fun title(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("title", parserName = "greedyString") title: String,
    ) = update(sender, id, interaction, joinState, "send-title") {
        it.sendTitle = (it.sendTitle ?: NpcConfig.TitleConfiguration()).copy(title = title)
    }

    @Command("$commandName edit <id> action subtitle <interaction> <joinState> <subtitle>")
    @Permission(COMMAND_PERMISSION)
    fun subtitle(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("subtitle", parserName = "greedyString") subtitle: String,
    ) = update(sender, id, interaction, joinState, "send-title") {
        it.sendTitle = (it.sendTitle ?: NpcConfig.TitleConfiguration()).copy(subtitle = subtitle)
    }

    @Command("$commandName edit <id> action title-duration <interaction> <joinState> <fadeIn> <stay> <fadeOut>")
    @Permission(COMMAND_PERMISSION)
    fun titleDuration(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("fadeIn") fadeIn: Int,
        @Argument("stay") stay: Int,
        @Argument("fadeOut") fadeOut: Int,
    ) {
        if (fadeIn < 0 || stay < 0 || fadeOut < 0) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Title durations cannot be negative."))
            return
        }
        update(sender, id, interaction, joinState, "title-duration") {
            val current = it.sendTitle ?: NpcConfig.TitleConfiguration()
            it.sendTitle = current.copy(fadeIn = fadeIn, stay = stay, fadeOut = fadeOut)
        }
    }

    @Command("$commandName edit <id> action send-to-server <interaction> <joinState> <server>")
    @Permission(COMMAND_PERMISSION)
    fun sendToServer(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("server", suggestions = "servers") server: String,
    ) = update(sender, id, interaction, joinState, "send-to-server") { it.sendToServer = server }

    @Command("$commandName edit <id> action transfer-to-server <interaction> <joinState> <address>")
    @Permission(COMMAND_PERMISSION)
    fun transferToServer(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("address") address: String,
    ) {
        if (!ADDRESS_PATTERN.matches(address)) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Address <#f8fafc>$address <#dc2626>is invalid."))
            return
        }
        update(sender, id, interaction, joinState, "transfer-to-server") { it.transferToServer = address }
    }

    @Command("$commandName edit <id> action clear <interaction> <joinState> <actionType>")
    @Permission(COMMAND_PERMISSION)
    fun clear(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interactionName: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("actionType", suggestions = "actionTypes") actionType: String,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val interaction = findPlayerInteraction(sender.sender, interactionName) ?: return
        val action = config.actions.firstOrNull {
            it.interactionType == interaction && it.joinState.equals(joinState, true)
        } ?: run {
            sender.sender.sendMessage(text("$PREFIX <#f59e0b>No action is configured for that interaction and join state."))
            return
        }
        if (!clear(action, actionType)) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Unknown action type <#f8fafc>$actionType<#dc2626>."))
            return
        }
        if (action.isEmpty()) config.actions.remove(action)
        saveConfig(sender.sender, config, "Cleared <#f8fafc>$actionType <#a3e635>for NPC <#f8fafc>$id<#a3e635>.", false)
    }

    @Command("$commandName edit <id> action clear <interaction> <joinState>")
    @Permission(COMMAND_PERMISSION)
    fun clearInteraction(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interactionName: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val interaction = findPlayerInteraction(sender.sender, interactionName) ?: return
        val removed = config.actions.removeIf {
            it.interactionType == interaction && it.joinState.equals(joinState, true)
        }
        if (!removed) {
            sender.sender.sendMessage(text("$PREFIX <#f59e0b>No action is configured for that interaction and join state."))
            return
        }
        saveConfig(sender.sender, config, "Cleared actions for NPC <#f8fafc>$id<#a3e635>.", false)
    }

    private fun update(
        sender: CommandSourceStack,
        id: String,
        interactionName: String,
        joinState: String,
        actionName: String,
        update: (NpcConfig.ActionConfiguration) -> Unit,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val interaction = findPlayerInteraction(sender.sender, interactionName) ?: return
        val state = joinState.lowercase()
        if (state.isBlank()) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Join state cannot be empty."))
            return
        }
        val action = config.actions.firstOrNull {
            it.interactionType == interaction && it.joinState.equals(state, true)
        } ?: NpcConfig.ActionConfiguration(interactionType = interaction, joinState = state)
            .also(config.actions::add)
        update(action)
        saveConfig(
            sender.sender,
            config,
            "Set <#f8fafc>$actionName <#a3e635>for NPC <#f8fafc>$id <#a3e635>on <#f8fafc>${interaction.name.lowercase()}<#a3e635>.",
            false,
        )
    }

    private fun clear(action: NpcConfig.ActionConfiguration, type: String): Boolean {
        return when (type.lowercase()) {
            "join-target" -> action.joinTarget = false
            "open-inventory" -> action.openInventory = null
            "play-sound" -> action.playSound = null
            "execute-command" -> action.executeCommand = null
            "send-message" -> action.sendMessage = null
            "teleport" -> action.teleport = null
            "send-title" -> action.sendTitle = null
            "send-to-server" -> action.sendToServer = null
            "transfer-to-server" -> action.transferToServer = null
            else -> return false
        }.let { true }
    }

    private fun NpcConfig.ActionConfiguration.isEmpty(): Boolean {
        return !joinTarget && openInventory == null && playSound == null && executeCommand == null &&
            sendMessage == null && teleport == null && sendTitle == null && sendToServer == null &&
            transferToServer == null
    }

    private fun NpcConfig.ActionConfiguration.configuredTypes(): List<String> = buildList {
        if (joinTarget) add("join-target")
        if (playSound != null) add("play-sound")
        if (executeCommand != null) add("execute-command")
        if (sendMessage != null) add("send-message")
        if (teleport != null) add("teleport")
        if (sendTitle != null) add("send-title")
        if (sendToServer != null) add("send-to-server")
        if (transferToServer != null) add("transfer-to-server")
    }

    companion object {
        private val ADDRESS_PATTERN = Regex("^[A-Za-z0-9.-]+(?::[0-9]{1,5})?$")
    }
}
