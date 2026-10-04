package app.simplecloud.npc.common.command.edit

import app.simplecloud.npc.common.command.AbstractNpcCommand
import app.simplecloud.npc.common.command.COMMAND_PERMISSION
import app.simplecloud.npc.common.command.COMMAND_SYNTAX
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.common.text.sendError
import app.simplecloud.npc.common.text.sendWarning
import app.simplecloud.npc.common.utils.InputChecks
import app.simplecloud.npc.core.config.ActionFields
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcCommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

class NpcActionCommand<C : NpcCommandSender>(pluginContext: NpcPluginContext) : AbstractNpcCommand<C>(pluginContext) {

    @Command("$COMMAND_SYNTAX edit <id> action")
    @Permission(COMMAND_PERMISSION)
    fun info(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return
        val visibleActions = config.actions.filter { it.configuredTypes().isNotEmpty() }.ifEmpty {
            sender.sendWarning("NPC {} has no actions.", id)
            return
        }

        val lines = buildList {
            add("${Msg.infoPrefix()}Actions for ${Msg.ACCENT}$id")
            visibleActions.forEach {
                val interaction = it.interactionType.name.lowercase()
                val types = it.configuredTypes().joinToString()
                add("   ${Msg.SUBTLE}- ${Msg.ACCENT}$interaction ${Msg.SUBTLE}(${it.joinState}) -> <#e2e8f0>$types")
            }
        }

        sender.sendMessage(Msg.miniMessage.deserialize(lines.joinToString("\n")))
    }

    @Command("$COMMAND_SYNTAX edit <id> action join-target <interaction> <joinState> <enabled>")
    @Permission(COMMAND_PERMISSION)
    fun joinTarget(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interactionName: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("enabled") enabled: Boolean,
    ) = update(sender, id, interactionName, joinState, ActionFields.JOIN_TARGET) { it.joinTarget = enabled }

    @Command("$COMMAND_SYNTAX edit <id> action open-inventory <interaction> <joinState> <inventoryId>")
    @Permission(COMMAND_PERMISSION)
    fun openInventory(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("inventoryId", suggestions = "inventoryIds") inventoryId: String,
    ) {
        findInventory(sender, inventoryId) ?: return

        update(sender, id, interaction, joinState, ActionFields.OPEN_INVENTORY) { it.openInventory = inventoryId }
    }

    @Command("$COMMAND_SYNTAX edit <id> action play-sound <interaction> <joinState> <sound>")
    @Permission(COMMAND_PERMISSION)
    fun playSound(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("sound", parserName = "greedyString") sound: String,
    ) {
        if (!InputChecks.isValidSound(sound)) {
            sender.sendError("Sound {} is invalid.", sound)
            return
        }

        update(sender, id, interaction, joinState, ActionFields.PLAY_SOUND) { it.playSound = sound }
    }

    @Command("$COMMAND_SYNTAX edit <id> action execute-command <interaction> <joinState> <command>")
    @Permission(COMMAND_PERMISSION)
    fun executeCommand(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("command", parserName = "greedyString") command: String,
    ) = update(sender, id, interaction, joinState, ActionFields.EXECUTE_COMMAND) {
        it.executeCommand = command.removePrefix("/")
    }

    @Command("$COMMAND_SYNTAX edit <id> action send-message <interaction> <joinState> <message>")
    @Permission(COMMAND_PERMISSION)
    fun sendMessage(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("message", parserName = "greedyString") message: String,
    ) = update(sender, id, interaction, joinState, ActionFields.SEND_MESSAGE) { it.sendMessage = message }

    @Command("$COMMAND_SYNTAX edit <id> action action-bar <interaction> <joinState> <text>")
    @Permission(COMMAND_PERMISSION)
    fun actionBar(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("text", parserName = "greedyString") text: String,
    ) = update(sender, id, interaction, joinState, ActionFields.ACTION_BAR) { it.actionBar = text }

    @Command("$COMMAND_SYNTAX edit <id> action teleport-here <interaction> <joinState>")
    @Permission(COMMAND_PERMISSION)
    fun teleportHere(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
    ) {
        val player = requirePlayer(sender) ?: return
        val location = player.location()

        update(sender, id, interaction, joinState, ActionFields.TELEPORT) { it.teleport = location }
    }

    @Command("$COMMAND_SYNTAX edit <id> action title <interaction> <joinState> <title>")
    @Permission(COMMAND_PERMISSION)
    fun title(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("title", parserName = "greedyString") title: String,
    ) = update(sender, id, interaction, joinState, "title") {
        it.sendTitle = (it.sendTitle ?: NpcConfig.TitleConfiguration()).copy(title = title)
    }

    @Command("$COMMAND_SYNTAX edit <id> action subtitle <interaction> <joinState> <subtitle>")
    @Permission(COMMAND_PERMISSION)
    fun subtitle(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("subtitle", parserName = "greedyString") subtitle: String,
    ) = update(sender, id, interaction, joinState, "subtitle") {
        it.sendTitle = (it.sendTitle ?: NpcConfig.TitleConfiguration()).copy(subtitle = subtitle)
    }

    @Command("$COMMAND_SYNTAX edit <id> action title-duration <interaction> <joinState> <fadeIn> <stay> <fadeOut>")
    @Permission(COMMAND_PERMISSION)
    fun titleDuration(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("fadeIn") fadeIn: Int,
        @Argument("stay") stay: Int,
        @Argument("fadeOut") fadeOut: Int,
    ) {
        val fadeRange = NpcConfig.TitleConfiguration.FADE_RANGE
        val stayRange = NpcConfig.TitleConfiguration.STAY_RANGE
        if (fadeIn !in fadeRange || fadeOut !in fadeRange || stay !in stayRange) {
            sender.sendError(
                "Fade ticks must be {}-{}, stay must be {}-{}.",
                fadeRange.first,
                fadeRange.last,
                stayRange.first,
                stayRange.last,
            )
            return
        }

        update(sender, id, interaction, joinState, "title-duration") {
            val current = it.sendTitle ?: NpcConfig.TitleConfiguration()
            it.sendTitle = current.copy(fadeIn = fadeIn, stay = stay, fadeOut = fadeOut)
        }
    }

    @Command("$COMMAND_SYNTAX edit <id> action send-to-server <interaction> <joinState> <server>")
    @Permission(COMMAND_PERMISSION)
    fun sendToServer(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("server", suggestions = "servers") server: String,
    ) = update(sender, id, interaction, joinState, ActionFields.SEND_TO_SERVER) { it.sendToServer = server }

    @Command("$COMMAND_SYNTAX edit <id> action transfer-to-server <interaction> <joinState> <address>")
    @Permission(COMMAND_PERMISSION)
    fun transferToServer(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interaction: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("address", parserName = "greedyString") address: String,
    ) {
        if (!InputChecks.isValidHostPort(address)) {
            sender.sendError("Address {} is invalid.", address)
            return
        }

        update(sender, id, interaction, joinState, ActionFields.TRANSFER_TO_SERVER) { it.transferToServer = address }
    }

    @Command("$COMMAND_SYNTAX edit <id> action clear <interaction> <joinState> <actionType>")
    @Permission(COMMAND_PERMISSION)
    fun clear(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interactionName: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("actionType", suggestions = "actionTypes") actionType: String,
    ) {
        val config = findNpc(sender, id) ?: return
        val interaction = findPlayerInteraction(sender, interactionName) ?: return
        val action = config.findAction(interaction, joinState) ?: run {
            sender.sendWarning("No action is configured for that interaction and join state.")
            return
        }
        if (!action.clearField(actionType)) {
            sender.sendError("Unknown action type {}.", actionType)
            return
        }
        config.dropActionIfEmpty(interaction, joinState)

        saveConfig(sender, config, "Cleared {} for NPC {}.", actionType, id, refreshHologram = false)
    }

    @Command("$COMMAND_SYNTAX edit <id> action clear <interaction> <joinState>")
    @Permission(COMMAND_PERMISSION)
    fun clearInteraction(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("interaction", suggestions = "playerInteractions") interactionName: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
    ) {
        val config = findNpc(sender, id) ?: return
        val interaction = findPlayerInteraction(sender, interactionName) ?: return
        val removed = config.actions.removeIf {
            it.interactionType == interaction && it.joinState.equals(joinState, ignoreCase = true)
        }
        if (!removed) {
            sender.sendWarning("No action is configured for that interaction and join state.")
            return
        }

        saveConfig(sender, config, "Cleared actions for NPC {}.", id, refreshHologram = false)
    }

    private fun update(
        sender: C,
        id: String,
        interactionName: String,
        joinState: String,
        actionName: String,
        update: (NpcConfig.ActionConfiguration) -> Unit,
    ) {
        val config = findNpc(sender, id) ?: return
        val interaction = findPlayerInteraction(sender, interactionName) ?: return
        update(config.actionOrCreate(interaction, joinState))
        saveConfig(
            sender,
            config,
            "Set {} for NPC {} on {}.",
            actionName,
            id,
            interaction.name.lowercase(),
            refreshHologram = false,
        )
    }
}
