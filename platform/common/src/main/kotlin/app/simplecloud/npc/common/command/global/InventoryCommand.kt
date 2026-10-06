package app.simplecloud.npc.common.command.global

import app.simplecloud.npc.common.command.AbstractNpcCommand
import app.simplecloud.npc.common.command.COMMAND_LABEL
import app.simplecloud.npc.common.command.COMMAND_PERMISSION
import app.simplecloud.npc.common.command.COMMAND_SYNTAX
import app.simplecloud.npc.common.command.INVENTORY_OPEN_OTHERS_PERMISSION
import app.simplecloud.npc.common.command.INVENTORY_OPEN_PERMISSION
import app.simplecloud.npc.common.inventory.source.InventoryOpenContext
import app.simplecloud.npc.common.manager.InventoryFailure
import app.simplecloud.npc.common.manager.InventoryOperationResult
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.text.sendError
import app.simplecloud.npc.common.text.sendInfo
import app.simplecloud.npc.common.text.sendSuccess
import app.simplecloud.npc.common.text.sendWarning
import app.simplecloud.npc.core.config.ConfigIds
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.platform.NpcCommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

class InventoryCommand<C : NpcCommandSender>(pluginContext: NpcPluginContext) : AbstractNpcCommand<C>(pluginContext) {

    @Command("$COMMAND_SYNTAX inventory create")
    @Permission(COMMAND_PERMISSION)
    fun createInteractive(sender: C) {
        val player = sender.asPlayer() ?: run {
            sender.sendInfo(
                "The create wizard is in-game only, use /{} inventory create <id> <rows> from the console.",
                COMMAND_LABEL,
            )
            return
        }

        pluginContext.editors.openInventoryCreate(player)
    }

    @Command("$COMMAND_SYNTAX inventory create <id> <rows>")
    @Permission(COMMAND_PERMISSION)
    fun create(sender: C, @Argument("id") id: String, @Argument("rows") rows: Int) {
        val config = when (val result = pluginContext.inventoryManager.create(id, rows)) {
            is InventoryOperationResult.Success -> result.config
            is InventoryOperationResult.Failure -> {
                sendFailure(sender, result)
                return
            }
        }

        sender.sendSuccess("Created inventory {}.", id)
        sender.asPlayer()?.let { player -> pluginContext.editors.openInventory(player, config) }
    }

    @Command("$COMMAND_SYNTAX inventory edit <id>")
    @Permission(COMMAND_PERMISSION)
    fun edit(sender: C, @Argument("id", suggestions = "inventoryIds") id: String) {
        val config = pluginContext.inventoryRepository.find(id) ?: run {
            sender.sendError(
                "Inventory {} was not found, create it with /{} inventory create {} <rows>.",
                id,
                COMMAND_LABEL,
                id,
            )
            return
        }

        val player = sender.asPlayer() ?: run {
            info(sender, config.id)
            sender.sendInfo("The editor is in-game only, run this as a player.")
            return
        }

        pluginContext.editors.openInventory(player, config)
    }

    @Command("$COMMAND_SYNTAX inventory delete <id>")
    @Permission(COMMAND_PERMISSION)
    fun delete(sender: C, @Argument("id", suggestions = "inventoryIds") id: String) {
        when (val result = pluginContext.inventoryManager.delete(id)) {
            is InventoryOperationResult.Success -> sender.sendSuccess("Deleted inventory {}.", id)
            is InventoryOperationResult.Failure -> sendFailure(sender, result)
        }
    }

    @Command("$COMMAND_SYNTAX inventory info <id>")
    @Permission(COMMAND_PERMISSION)
    fun info(sender: C, @Argument("id", suggestions = "inventoryIds") id: String) {
        val config = findInventory(sender, id) ?: return
        val liveGroups = config.liveGroups()

        sender.sendInfo(
            "Inventory {}: {} rows, {} static items, {} live group(s).",
            config.id,
            config.rows,
            config.items.count { it.liveGroup == null },
            liveGroups.size,
        )

        liveGroups.forEach { group ->
            sender.sendInfo(
                "Live group {}: source {}, {} slots{}.",
                group.id,
                group.sourceType ?: "(no source set)",
                group.contentSlots,
                if (group.paginated) ", paginated" else "",
            )
        }
    }

    @Command("$COMMAND_SYNTAX inventory open <id> [player]")
    @Permission(INVENTORY_OPEN_PERMISSION)
    fun open(
        sender: C,
        @Argument("id", suggestions = "inventoryIds") id: String,
        @Argument("player", suggestions = "onlinePlayers") playerName: String?,
    ) {
        val config = findInventory(sender, id) ?: return
        val target = if (playerName == null) {
            requirePlayer(sender) ?: return
        } else {
            if (!sender.hasPermission(INVENTORY_OPEN_OTHERS_PERMISSION)) {
                sender.sendError("You may only open menus for yourself.")
                return
            }
            pluginContext.playerDirectory.findOnlinePlayer(playerName) ?: run {
                sender.sendError("Player {} is not online.", playerName)
                return
            }
        }

        val permission = config.openPermission
        if (playerName != null && !permission.isNullOrBlank() && !target.hasPermission(permission)) {
            sender.sendError("{} lacks {} to open {}.", target.name, permission, config.id)
            return
        }

        pluginContext.inventoryViews.open(target, config.id, InventoryOpenContext(emptyList()))
        if (playerName != null) sender.sendSuccess("Opened {} for {}.", config.id, target.name)
    }

    @Command("$COMMAND_SYNTAX inventory list")
    @Permission(COMMAND_PERMISSION)
    fun list(sender: C) {
        val inventories = pluginContext.inventoryRepository.ids().ifEmpty {
            sender.sendWarning("No inventories were found.")
            return
        }

        sender.sendInfo("Configured inventories ({}): {}", inventories.size, inventories.joinToString())
    }

    private fun sendFailure(sender: C, result: InventoryOperationResult.Failure) = when (result.failure) {
        InventoryFailure.INVALID_ID -> sender.sendError("Invalid id {}. {}.", result.detail, ConfigIds.DESCRIPTION)
        InventoryFailure.ALREADY_EXISTS -> sender.sendError("Inventory {} already exists.", result.detail)
        InventoryFailure.INVALID_ROWS -> sender.sendError(
            "Rows must be between {} and {}.",
            InventoryConfiguration.MIN_ROWS,
            InventoryConfiguration.MAX_ROWS,
        )
        InventoryFailure.NOT_FOUND -> sender.sendError("Inventory {} was not found.", result.detail)
    }
}
