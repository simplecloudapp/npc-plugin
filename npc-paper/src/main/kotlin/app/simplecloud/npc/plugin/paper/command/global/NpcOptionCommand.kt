package app.simplecloud.npc.plugin.paper.command.global

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.plugin.paper.command.text
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.config.NpcOption
import app.simplecloud.npc.shared.namespace.NpcNamespace
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

class NpcOptionCommand(
    namespace: NpcNamespace
) : AbstractNpcCommand(
    namespace
) {

    @Command("$commandName <id> setOption <key> <value>")
    @Permission("")
    fun executeSetOption(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("key") key: String,
        @Argument("value") value: String,
    ) {
        val player = sender.sender as Player
        invokeConfig(player, npcId) { config ->
            config.options.removeIf { it.key == key }
            config.options.add(NpcOption(key, value))
            player.sendMessage(text("$PREFIX <#ffffff>A new option $key with value $value has been <#a3e635>created."))
            config
        }
    }

    @Command("$commandName <id> removeOption <key>")
    @Permission("")
    fun executeRemoveOption(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("key", suggestions = "globalOptionKeys") key: String
    ) {
        val player = sender.sender as Player
        invokeConfig(player, npcId) { config ->
            config.options.removeIf { it.key == key }
            player.sendMessage(text("$PREFIX <#ffffff>Option $key has been <#dc2626>removed!"))
            config
        }
    }

    @Command("$commandName <id> getOption <key>")
    @Permission("")
    fun executeGetOption(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("key", suggestions = "globalOptionKeys") key: String
    ) {
        val player = sender.sender as Player

        val npcConfig = findNpcConfigById(player, npcId) ?: return
        val option = npcConfig.getOption(key)
        CommandMessages.sendOptionMessage(player, npcConfig, key, option)
    }

    @Suggestions("globalOptionKeys")
    fun suggestGlobalOptionKey(context: CommandContext<CommandSourceStack>): List<String> {
        val text = context.rawInput().input().split(" ")
        val npcConfig = this.namespace.npcRepository.get(text[1]) ?: return emptyList()
        return npcConfig.options.map { it.key }
    }

    private fun invokeConfig(player: Player, npcId: String, function: (NpcConfig) -> NpcConfig) {
        val npcConfig = findNpcConfigById(player, npcId) ?: return
        val newConfig = function(npcConfig)
        this.namespace.npcRepository.save("${newConfig.id}.yml", newConfig)
    }

}