package app.simplecloud.npc.shared.inventory.configuration

import app.simplecloud.api.server.ServerState
import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig.PaginationInventory.PaginationItem
import app.simplecloud.npc.shared.utils.ConfigVersion
import org.bukkit.event.inventory.ClickType

/**
 * @author Niklas Nieberler
 */

object DefaultInventoryConfiguration {

    private val pagination = InventoryConfig.PaginationInventory(
        listedGroupName = "lobby",
        listedPersistentServers = emptyList(),
        serverNamePattern = "<group_name>-<numerical_id>",
        stateItems = mapOf(
            ServerState.PREPARING to "starting_pagination_item",
            ServerState.STARTING to "starting_pagination_item",
            ServerState.AVAILABLE to "available_pagination_item",
            ServerState.INGAME to "ingame_pagination_item"
        ),
        fromSlot = InventoryConfig.ItemSlot(1, 1),
        toSlot = InventoryConfig.ItemSlot(3, 7),
        excludedSlots = listOf(
            InventoryConfig.ItemSlot(1, 8),
            InventoryConfig.ItemSlot(2, 0),
            InventoryConfig.ItemSlot(2, 8),
            InventoryConfig.ItemSlot(3, 0)
        ),
        nextPageItem = PaginationItem(
            item = "next_pagination_item",
            slot = InventoryConfig.ItemSlot(4, 0),
            increments = mapOf(ClickType.LEFT to 1)
        ),
        previousPageItem = PaginationItem(
            item = "previous_pagination_item",
            slot = InventoryConfig.ItemSlot(4, 0),
            increments = mapOf(ClickType.LEFT to -1)
        )
    )

    val defaultInventoryConfig = InventoryConfig(
        version = ConfigVersion.VERSION,
        id = "default",
        npcType = null,
        title = "<#ffffff>Inventory",
        rows = 6,
        pagination = pagination,
        staticSlots = listOf(
            InventoryConfig.StaticItem(
                item = "red_previous_pagination_item",
                slots = listOf(InventoryConfig.ItemSlot(5, 0)),
                fromSlot = null,
                toSlot = null
            ),
            InventoryConfig.StaticItem(
                item = "red_next_pagination_item",
                slots = listOf(InventoryConfig.ItemSlot(5, 8)),
                fromSlot = null,
                toSlot = null
            )
        ),
        items = listOf(
            InventoryConfig.InventoryItem(
                id = "next_pagination_item",
                material = "LIME_STAINED_GLASS_PANE",
                displayName = "<green>Next Page"
            ),
            InventoryConfig.InventoryItem(
                id = "previous_pagination_item",
                material = "LIME_STAINED_GLASS_PANE",
                displayName = "<green>Previous Page"
            ),
            InventoryConfig.InventoryItem(
                id = "red_next_pagination_item",
                material = "RED_STAINED_GLASS_PANE",
                displayName = "<red>Next Page"
            ),
            InventoryConfig.InventoryItem(
                id = "red_previous_pagination_item",
                material = "RED_STAINED_GLASS_PANE",
                displayName = "<red>Previous Page"
            ),
            InventoryConfig.InventoryItem(
                id = "starting_pagination_item",
                material = "ORANGE_BANNER",
                displayName = "<group_name> <numerical_id>"
            ),
            InventoryConfig.InventoryItem(
                id = "available_pagination_item",
                material = "LIME_BANNER",
                displayName = "<group_name> <numerical_id>",
                customModelData = null,
                lores = listOf(
                    "<gray>Players: <#38bdf8><online_players><dark_gray><dark_gray>/<#38bdf8><max_players>",
                    "",
                )
            ),
            InventoryConfig.InventoryItem(
                id = "ingame_pagination_item",
                material = "BROWN_BANNER",
                displayName = "<group_name> <numerical_id>"
            )
        )
    )

}