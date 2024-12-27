package app.simplecloud.npc.shared.inventory.configuration

import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig.PaginationInventory.PaginationItem
import build.buf.gen.simplecloud.controller.v1.ServerState
import org.bukkit.event.inventory.ClickType

/**
 * @author Niklas Nieberler
 */

object DefaultInventoryConfiguration {

    private val pagination = InventoryConfig.PaginationInventory(
        "lobby",
        "<group_name>-<numerical_id>",
        mapOf(
            ServerState.PREPARING to "starting_pagination_item",
            ServerState.STARTING to "starting_pagination_item",
            ServerState.AVAILABLE to "available_pagination_item",
            ServerState.INGAME to "ingame_pagination_item"
        ),
        InventoryConfig.ItemSlot(1, 1),
        InventoryConfig.ItemSlot(3, 7),
        listOf(
            InventoryConfig.ItemSlot(1, 8),
            InventoryConfig.ItemSlot(2, 0),
            InventoryConfig.ItemSlot(2, 8),
            InventoryConfig.ItemSlot(3, 0)
        ),
        PaginationItem(
            "next_pagination_item",
            InventoryConfig.ItemSlot(4, 0),
            mapOf(ClickType.LEFT to 1)
        ),
        PaginationItem(
            "previous_pagination_item",
            InventoryConfig.ItemSlot(4, 0),
            mapOf(ClickType.LEFT to -1)
        )
    )

    val defaultInventoryConfig = InventoryConfig(
        "default",
        "<#ffffff>Inventory",
        6,
        pagination,
        listOf(
            InventoryConfig.StaticItem(
                "red_previous_pagination_item",
                listOf(InventoryConfig.ItemSlot(5, 0)),
                null,
                null
            ),
            InventoryConfig.StaticItem(
                "red_next_pagination_item",
                listOf(InventoryConfig.ItemSlot(5, 8)),
                null,
                null
            )
        ),
        listOf(
            InventoryConfig.InventoryItem(
                "next_pagination_item",
                "LIME_STAINED_GLASS_PANE",
                "<green>Nächste Seite"
            ),
            InventoryConfig.InventoryItem(
                "previous_pagination_item",
                "LIME_STAINED_GLASS_PANE",
                "<green>Vorherige Seite"
            ),
            InventoryConfig.InventoryItem(
                "red_next_pagination_item",
                "RED_STAINED_GLASS_PANE",
                "<red>Nächste Seite"
            ),
            InventoryConfig.InventoryItem(
                "red_previous_pagination_item",
                "RED_STAINED_GLASS_PANE",
                "<red>Vorherige Seite"
            ),
            InventoryConfig.InventoryItem(
                "starting_pagination_item",
                "ORANGE_BANNER",
                "<group_name> <numerical_id>"
            ),
            InventoryConfig.InventoryItem(
                "available_pagination_item",
                "LIME_BANNER",
                "<group_name> <numerical_id>",
                lores = listOf(
                    "<gray>Players: <#38bdf8><player_count><dark_gray><dark_gray>/<#38bdf8><max_players>",
                    "",
                    "<gray>Version: <#a3a3a3><property:server-software> <property:minecraft-version>",
                    "<gray>Template: <#a3a3a3><property:template-id>"
                )
            ),
            InventoryConfig.InventoryItem(
                "ingame_pagination_item",
                "BROWN_BANNER",
                "<group_name> <numerical_id>"
            )
        )
    )

}