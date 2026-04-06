package app.simplecloud.npc.plugin.paper.command.global

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.cloud.CloudService
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.plugin.api.shared.extension.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
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

    private val cloudApi = CloudService.cloudApi

    @Command(commandName)
    @Permission("simplecloud.command.npc")
    fun execute(sender: CommandSourceStack) {
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
        val player = sender.sender as Player
        CommandMessages.sendHelpMessage(player)
    }

    @Command("$commandName apply <id>")
    @Permission("simplecloud.command.npc")
    fun executeApplyNpc(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "availableNpcIds") npcId: String
    ) {
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
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

    @Command("$commandName <id>")
    @Permission("simplecloud.command.npc")
    fun executeNpc(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String
    ) {
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
        val player = sender.sender as Player
        CommandMessages.sendHelpMessage(player)
    }

    @Command("$commandName <id> setHologramServerBase <name>")
    @Permission("simplecloud.command.npc")
    fun executeHologramGroup(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") npcId: String,
        @Argument("name", suggestions = "serverBases") name: String
    ) {
        val player = sender.sender as Player
        invokeConfig(player, npcId) { config ->
            config.hologramConfiguration.placeholderServerBaseName = name
            player.sendMessage(text("$PREFIX <#ffffff>HologramServerBase $name has been <#a3e635>created."))
            CoroutineScope(Dispatchers.IO).launch { namespace.hologramManager.updateTextHologramByGroup(config, name) }
            config
        }
    }

    @Suggestions("serverBases")
    fun suggestServerBaseNames(): List<String> {
        return runBlocking {
            buildList {
                addAll(cloudApi.group().allGroups.await().map { it.name })
                addAll(cloudApi.server().allServers.await().map { it.group.name + "-" + it.numericalId })
                addAll(cloudApi.persistentServer().allPersistentServers.await().map { it.name })
            }
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
        val serverBaseName = npcConfig.hologramConfiguration.placeholderServerBaseName
        coroutineScope.launch { hologramManager.updateTextHologramByGroup(npcConfig, serverBaseName) }
    }

    private fun invokeConfig(player: Player, npcId: String, function: (NpcConfig) -> NpcConfig) {
        val npcConfig = findNpcConfigById(player, npcId) ?: return
        val newConfig = function(npcConfig)
        this.namespace.npcRepository.save(newConfig)
    }

}