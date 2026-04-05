package app.simplecloud.npc.plugin.paper.command.global

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.shared.inventory.configuration.DefaultInventoryConfiguration
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

class NpcInventoryCommand(
    namespace: NpcNamespace
) : AbstractNpcCommand(
    namespace
) {

    @Command("$commandName openInventory <name>")
    @Permission("simplecloud.command.npc")
    fun executeOpenInventory(
        sender: CommandSourceStack,
        @Argument("name", suggestions = "inventories") id: String,
    ) {
        val player = sender.sender as Player
        this.namespace.inventoryManager.openInventory(player, id)
    }

    @Command("$commandName createInventory <name> <groupName>")
    @Permission("simplecloud.command.npc")
    fun executeCreateInventory(
        sender: CommandSourceStack,
        @Argument("name") id: String,
        @Argument("groupName", suggestions = "groupNames") groupName: String,
    ) {
        val player = sender.sender as Player
        val inventoryConfig = DefaultInventoryConfiguration.defaultInventoryConfig
        val repository = this.namespace.inventoryRepository
        if (repository.get(id) != null) {
            player.sendMessage(text("$PREFIX <#dc2626>Inventory with id $id already exist!"))
            return
        }
        inventoryConfig.id = id
        inventoryConfig.pagination?.listedGroupName = groupName
        repository.save("${id}.yml", inventoryConfig)
        player.sendMessage(text("$PREFIX <#ffffff>Inventory $id has been <#a3e635>created."))
    }

    @Suggestions("inventories")
    fun suggestInventories(): List<String> {
        return this.namespace.inventoryRepository.getAll().map { it.id }
    }
}