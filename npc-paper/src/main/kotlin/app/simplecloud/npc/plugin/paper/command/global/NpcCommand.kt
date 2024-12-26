package app.simplecloud.npc.plugin.paper.command.global

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.config.NpcOption
import app.simplecloud.npc.shared.controller.ControllerService
import app.simplecloud.npc.shared.hologram.HologramOptions
import app.simplecloud.npc.shared.inventory.NpcInventory
import app.simplecloud.npc.shared.inventory.configuration.InventoryRepository
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

    val inventoryRepository = InventoryRepository()
    init {
        inventoryRepository.load()
    }

    @Command(commandName)
    @Permission("simplecloud.command.npc")
    fun execute(sender: CommandSourceStack) {
        val player = sender.sender as Player
        CommandMessages.sendHelpMessage(player)

        CoroutineScope(Dispatchers.IO).launch {
            inventoryRepository.getAll().forEach {
                NpcInventory(it).open(player)
            }
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
        val optionName = HologramOptions.PLACEHOLDER_GROUP_NAME.first
        invokeConfig(player, npcId) { config ->
            config.options.removeIf { it.key == optionName }
            config.options.add(NpcOption(optionName, name))
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

    private fun invokeConfig(player: Player, npcId: String, function: (NpcConfig) -> NpcConfig) {
        val npcConfig = findNpcConfigById(player, npcId) ?: return
        val newConfig = function(npcConfig)
        this.namespace.npcRepository.save("${newConfig.id}.yml", newConfig)
    }

}