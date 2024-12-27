package app.simplecloud.npc.plugin.paper.command.interact

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions

/**
 * @author Niklas Nieberler
 */

class NpcInteractCommand(
    namespace: NpcNamespace
) : AbstractNpcCommand(
    namespace
) {

    private val repository = namespace.npcRepository

    @Command("$commandName <id> interact <playerInteraction>")
    @Permission("simplecloud.command.npc")
    fun executeInteraction(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("playerInteraction", suggestions = "playerInteractions") playerInteraction: String
    ) {
        val player = sender.sender as Player

        val npcConfig = findNpcConfigById(player, npcId) ?: return
        val interaction = findPlayerInteraction(player, playerInteraction) ?: return

        val npcInteraction = npcConfig.getPlayerInteraction(interaction)
        if (npcInteraction == null) {
            CommandMessages.sendNoActionCreated(player, playerInteraction)
            return
        }

        player.sendMessage(
            text("$PREFIX <#ffffff>Information of npc ${npcConfig.id} <#737373>(${npcInteraction.playerInteraction.name.lowercase()})").appendNewline()
                .append(text("   <#a3a3a3>Action: <#38bdf8>${npcInteraction.action.name.lowercase()}")).appendNewline()
                .append(text("   <#a3a3a3>Options: <#38bdf8>${npcInteraction.getOptions().joinToString(", ") { "${it.key}=${it.value}" }}"))
        )
    }

    @Command("$commandName <id> interact <playerInteraction> setAction <action>")
    @Permission("simplecloud.command.npc")
    fun executeInteractionSetAction(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("playerInteraction", suggestions = "playerInteractions") playerInteraction: String,
        @Argument("action", suggestions = "actions") action: String,
    ) {
        val player = sender.sender as Player

        val npcConfig = findNpcConfigById(player, npcId) ?: return
        val interaction = findPlayerInteraction(player, playerInteraction) ?: return
        val npcAction = findNpcAction(player, action) ?: return

        val newInteraction = npcConfig.getPlayerInteraction(playerInteraction) ?: NpcConfig.NpcInteraction(
            interaction,
            npcAction
        )

        newInteraction.action = npcAction
        npcConfig.updateAction(newInteraction)

        this.repository.save("${npcConfig.id}.yml", npcConfig)

        player.sendMessage(text("$PREFIX <#ffffff>Action ${npcAction.name.lowercase()} for npc ${npcConfig.id} has been <#a3e635>created."))
    }

    fun findNpcAction(player: Player, action: String): Action? {
        val action = Action.getOrNull(action)
        if (action == null) {
            player.sendMessage(text("$PREFIX <#dc2626>This action does not exist! Please use ${Action.entries.joinToString(", ") { it.name.lowercase() }}"))
            return null
        }
        return action
    }

    @Suggestions("actions")
    fun suggestAction(): List<String> {
        return Action.entries.map { it.name.lowercase() }
    }

}