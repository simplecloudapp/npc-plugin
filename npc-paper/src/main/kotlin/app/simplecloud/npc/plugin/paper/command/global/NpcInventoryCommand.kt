package app.simplecloud.npc.plugin.paper.command.global

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.shared.cloud.CloudService
import app.simplecloud.npc.shared.enums.NpcType
import app.simplecloud.npc.shared.inventory.configuration.DefaultInventoryConfiguration
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
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
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
        val player = sender.sender as Player
        this.namespace.inventoryManager.openInventory(player, id)
    }

    @Command("$commandName createInventory <name> group <group>")
    @Permission("simplecloud.command.npc")
    fun executeCreateInventoryGroup(
        sender: CommandSourceStack,
        @Argument("name") id: String,
        @Argument("group", suggestions = "groupNames") groupName: String,
    ) {
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
        val player = sender.sender as Player
        val repository = this.namespace.inventoryRepository

        if (repository.get(id) != null) {
            player.sendMessage(text("$PREFIX <#dc2626>Inventory with id $id already exists!"))
            return
        }

        val groupExists = runBlocking {
            try {
                CloudService.cloudApi.group().getGroupByName(groupName).await()
                true
            } catch (e: Exception) {
                false
            }
        }

        if (!groupExists) {
            player.sendMessage(text("$PREFIX <#dc2626>Group with name $groupName does not exist!"))
            return
        }

        val inventoryConfig = DefaultInventoryConfiguration.defaultInventoryConfig.copy(
            id = id,
            npcType = NpcType.GROUP,
            pagination = DefaultInventoryConfiguration.defaultInventoryConfig.pagination?.copy(
                listedGroupName = groupName
            )
        )
        repository.save("${id}.yml", inventoryConfig)
        player.sendMessage(text("$PREFIX <#ffffff>Inventory $id for group <#a3e635>$groupName <#ffffff>has been created."))
    }

    @Command("$commandName createInventory <name> persistent <server>")
    @Permission("simplecloud.command.npc")
    fun executeCreateInventoryPersistent(
        sender: CommandSourceStack,
        @Argument("name") id: String,
        @Argument("server", suggestions = "persistentServerNames") serverName: String,
    ) {
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
        val player = sender.sender as Player
        val repository = this.namespace.inventoryRepository

        if (repository.get(id) != null) {
            player.sendMessage(text("$PREFIX <#dc2626>Inventory with id $id already exists!"))
            return
        }

        val serverExists = runBlocking {
            try {
                CloudService.cloudApi.persistentServer().getPersistentServerByName(serverName).await()
                true
            } catch (e: Exception) {
                false
            }
        }

        if (!serverExists) {
            player.sendMessage(text("$PREFIX <#dc2626>Persistent server with name $serverName does not exist!"))
            return
        }

        val inventoryConfig = DefaultInventoryConfiguration.defaultInventoryConfig.copy(
            id = id,
            npcType = NpcType.PERSISTENT,
            pagination = DefaultInventoryConfiguration.defaultInventoryConfig.pagination?.copy(
                listedPersistentServers = listOf(serverName)
            )
        )
        repository.save("${id}.yml", inventoryConfig)
        player.sendMessage(text("$PREFIX <#ffffff>Inventory $id for persistent server <#a3e635>$serverName <#ffffff>has been created."))
    }

    @Command("$commandName addInventoryPersistentServer <name> <server>")
    @Permission("simplecloud.command.npc")
    fun executeAddInventoryPersistentServer(
        sender: CommandSourceStack,
        @Argument("name", suggestions = "persistentInventories") id: String,
        @Argument("server", suggestions = "persistentServerNames") serverName: String,
    ) {
        if (sender.sender !is Player) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be executed by a player!"))
            return
        }
        val player = sender.sender as Player
        val repository = this.namespace.inventoryRepository

        val inventoryConfig = repository.get(id)
        if (inventoryConfig == null) {
            player.sendMessage(text("$PREFIX <#dc2626>Inventory with id $id does not exist!"))
            return
        }

        if (inventoryConfig.npcType != NpcType.PERSISTENT) {
            player.sendMessage(text("$PREFIX <#dc2626>This inventory is not of type persistent!"))
            return
        }

        val serverExists = runBlocking {
            try {
                CloudService.cloudApi.persistentServer().getPersistentServerByName(serverName).await()
                true
            } catch (e: Exception) {
                false
            }
        }

        if (!serverExists) {
            player.sendMessage(text("$PREFIX <#dc2626>Persistent server with name $serverName does not exist!"))
            return
        }

        val pagination = inventoryConfig.pagination
        if (pagination == null) {
            player.sendMessage(text("$PREFIX <#dc2626>Inventory has no pagination configuration!"))
            return
        }

        if (pagination.listedPersistentServers.contains(serverName)) {
            player.sendMessage(text("$PREFIX <#dc2626>Server $serverName is already in the inventory!"))
            return
        }

        val updatedConfig = inventoryConfig.copy(
            pagination = pagination.copy(
                listedPersistentServers = pagination.listedPersistentServers + serverName
            )
        )
        repository.save("${id}.yml", updatedConfig)
        player.sendMessage(text("$PREFIX <#ffffff>Persistent server <#a3e635>$serverName <#ffffff>has been added to inventory $id."))
    }

    @Suggestions("inventories")
    fun suggestInventories(): List<String> {
        return this.namespace.inventoryRepository.getAll().map { it.id }
    }

    @Suggestions("persistentInventories")
    fun suggestPersistentInventories(): List<String> {
        return this.namespace.inventoryRepository.getAll()
            .filter { it.npcType == NpcType.PERSISTENT }
            .map { it.id }
    }

    @Suggestions("groupNames")
    fun suggestGroupNames(): List<String> {
        return runBlocking {
            try {
                CloudService.cloudApi.group().allGroups.await().map { it.name }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    @Suggestions("persistentServerNames")
    fun suggestPersistentServerNames(): List<String> {
        return runBlocking {
            try {
                CloudService.cloudApi.persistentServer().allPersistentServers.await().map { it.name }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}