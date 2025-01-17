package app.simplecloud.npc.plugin.paper.command.global

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.controller.ControllerService
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions

/**
 * @author Niklas Nieberler
 */

class NpcCommand(
    namespace: NpcNamespace
) : AbstractNpcCommand(
    namespace
) {

    @Command(commandName)
    @Permission("simplecloud.command.npc")
    fun execute(sender: CommandSourceStack) {
        val player = sender.sender as Player
        CommandMessages.sendHelpMessage(player)
    }

    @Command("$commandName apply <id>")
    @Permission("simplecloud.command.npc")
    fun executeApplyNpc(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "availableNpcIds") npcId: String
    ) {
        val player = sender.sender as Player
        val npcManager = this.namespace.npcManager

        if (!this.namespace.existNpc(npcId)) {
            player.sendMessage(text("$PREFIX <#dc2626>Npc with id $npcId does not exist!"))
            return
        }

        if (npcManager.exist(npcId)) {
            player.sendMessage(text("$PREFIX <#dc2626>Npc with id $npcId already exist!"))
            return
        }

        val npcConfig = npcManager.create(npcId) ?: return
        player.sendMessage(text("$PREFIX <#ffffff>Npc with id $npcId has been <#a3e635>created."))
        CoroutineScope(Dispatchers.IO).launch {
            updateHolograms(npcConfig)
        }
    }

    @Command("$commandName <id> setHologramGroup <name>")
    @Permission("simplecloud.command.npc")
    fun executeHologramGroup(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("name", suggestions = "groupNames") name: String
    ) {
        val player = sender.sender as Player
        invokeConfig(player, npcId) { config ->
            config.hologramConfiguration.placeholderGroupName = name
            player.sendMessage(text("$PREFIX <#ffffff>HologramGroup $name has been <#a3e635>created."))
            CoroutineScope(Dispatchers.IO).launch { namespace.hologramManager.updateTextHologramByGroup(config, name) }
            config
        }
    }

    @Suggestions("groupNames")
    fun suggestGroupNames(): List<String> {
        return runBlocking {
            ControllerService.controllerApi.getGroups().getAllGroups().map { it.name }
        }
    }

    @Suggestions("availableNpcIds")
    fun suggestAvailableNpcId(): List<String> {
        return this.namespace.findAllNpcs()
            .filter { !this.namespace.npcManager.exist(it) }
    }

    private fun updateHolograms(npcConfig: NpcConfig) {
        val hologramManager = this.namespace.hologramManager
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        if (hologramManager.getTextDisplays(npcConfig.id).isEmpty()) {
            coroutineScope.launch { hologramManager.updateHolograms(npcConfig) }
            return
        }
        val groupName = npcConfig.hologramConfiguration.placeholderGroupName
        coroutineScope.launch { hologramManager.updateTextHologramByGroup(npcConfig, groupName) }
    }

    private fun invokeConfig(player: Player, npcId: String, function: (NpcConfig) -> NpcConfig) {
        val npcConfig = findNpcConfigById(player, npcId) ?: return
        val newConfig = function(npcConfig)
        this.namespace.npcRepository.save("${newConfig.id}.yml", newConfig)
    }

}