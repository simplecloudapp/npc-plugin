package app.simplecloud.npc.plugin.paper.command.interact

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.shared.text
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.config.NpcOption
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.player.PlayerActions
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext

/**
 * @author Niklas Nieberler
 */

class NpcInteractOptionCommand(
    namespace: NpcNamespace
) : AbstractNpcCommand(
    namespace
) {

    @Command("$commandName <id> interact <playerInteraction> setOption <key> <value>")
    @Permission("")
    fun executeInteractionSetOption(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("playerInteraction", suggestions = "playerInteractions") playerInteraction: String,
        @Argument("key", suggestions = "actionOptionKeys") key: String,
        @Argument("value") value: String,
    ) {
        val player = sender.sender as Player
        invokeConfig(player, npcId, playerInteraction) { config, interaction ->
            interaction.options.removeIf { it.key == key }
            interaction.options.add(NpcOption(key, value))
            player.sendMessage(text("$PREFIX <#ffffff>A new option $key with value $value has been <#a3e635>created."))
            config.updateAction(interaction)
        }
    }

    @Command("$commandName <id> interact <playerInteraction> removeOption <key>")
    @Permission("")
    fun executeInteractionRemoveOption(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("playerInteraction", suggestions = "playerInteractions") playerInteraction: String,
        @Argument("key", suggestions = "optionKeys") key: String
    ) {
        val player = sender.sender as Player
        invokeConfig(player, npcId, playerInteraction) { config, interaction ->
            interaction.options.removeIf { it.key == key }
            player.sendMessage(text("$PREFIX <#ffffff>Option $key has been <#dc2626>removed!"))
            config.updateAction(interaction)
        }
    }

    @Command("$commandName <id> interact <playerInteraction> getOption <key>")
    @Permission("")
    fun executeInteractionGetOption(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("playerInteraction", suggestions = "playerInteractions") playerInteraction: String,
        @Argument("key", suggestions = "optionKeys") key: String
    ) {
        val player = sender.sender as Player

        val npcConfig = findNpcConfigById(player, npcId) ?: return
        findPlayerInteraction(player, playerInteraction) ?: return

        val newInteraction = npcConfig.getPlayerInteraction(playerInteraction)
        if (newInteraction == null) {
            CommandMessages.sendNoActionCreated(player, playerInteraction)
            return
        }

        val option = newInteraction.getOption(key)
        CommandMessages.sendOptionMessage(player, npcConfig, key, option)
    }

    @Suggestions("actionOptionKeys")
    fun suggestActionOptionKey(context: CommandContext<CommandSourceStack>): List<String> {
        val optionKeys = PlayerActions.getAllOptionKeys()
        val text = context.rawInput().input().split(" ")
        val npcConfig = this.namespace.npcRepository.get(text[1]) ?: return optionKeys
        return listOf(*(npcConfig.getPlayerInteraction(text[3])?.action?.actionHandler?.getOptions()?.map { it.first }
            ?: emptyList()).toTypedArray(), *optionKeys.toTypedArray())
    }

    @Suggestions("optionKeys")
    fun suggestOptionKey(context: CommandContext<CommandSourceStack>): List<String> {
        val text = context.rawInput().input().split(" ")
        val npcConfig = this.namespace.npcRepository.get(text[1]) ?: return emptyList()
        return npcConfig.getPlayerInteraction(text[3])?.options?.map { it.key } ?: emptyList()
    }

    private fun invokeConfig(
        player: Player,
        npcId: String,
        playerInteraction: String,
        function: (NpcConfig, NpcConfig.NpcInteraction) -> NpcConfig
    ) {
        val npcConfig = findNpcConfigById(player, npcId) ?: return
        findPlayerInteraction(player, playerInteraction) ?: return
        val newInteraction = npcConfig.getPlayerInteraction(playerInteraction)
        if (newInteraction == null) {
            CommandMessages.sendNoActionCreated(player, playerInteraction)
            return
        }
        val newConfig = function(npcConfig, newInteraction)
        this.namespace.npcRepository.save("${newConfig.id}.yml", newConfig)
    }

}