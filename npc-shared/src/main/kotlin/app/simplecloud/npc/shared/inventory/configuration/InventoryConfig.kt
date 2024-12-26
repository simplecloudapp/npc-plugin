package app.simplecloud.npc.shared.inventory.configuration

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.config.NpcOption
import build.buf.gen.simplecloud.controller.v1.ServerState
import org.bukkit.event.inventory.ClickType
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Niklas Nieberler
 */

@ConfigSerializable
data class InventoryConfig(
    val id: String = "",
    val title: String = "",
    val rows: Int = 6,
    val pagination: PaginationInventory? = null,
    val staticSlots: List<StaticItem> = emptyList(),
    val items: List<InventoryItem> = emptyList()
) {

    @ConfigSerializable
    data class StaticItem(
        val item: String = "",
        val slots: List<ItemSlot> = listOf(),
        val fromSlot: ItemSlot = ItemSlot(),
        val toSlot: ItemSlot = ItemSlot()
    )

    @ConfigSerializable
    data class PaginationInventory(
        val listedGroupName: String = "lobby",
        val serverNamePattern: String = "<group_name>-<numerical_id>",
        val stateItems: Map<ServerState, String> = emptyMap(),
        val fromSlot: ItemSlot = ItemSlot(),
        val toSlot: ItemSlot = ItemSlot(),
        val excludedSlots: List<ItemSlot> = emptyList(),
        val nextPageItem: PaginationItem = PaginationItem(),
        val previousPageItem: PaginationItem = PaginationItem()
    ) {
        @ConfigSerializable
        data class PaginationItem(
            val item: String = "",
            val slot: ItemSlot = ItemSlot(),
            val increments: Map<ClickType, Int> = emptyMap()
        )
    }

    @ConfigSerializable
    data class InventoryItem(
        val id: String = "",
        val material: String = "STONE",
        val displayName: String = "",
        val customModelData: Int? = null,
        val lores: List<String> = emptyList()
    )

    @ConfigSerializable
    data class ItemSlot(
        val row: Int = -1,
        val column: Int = -1
    ) {
        fun isBlank(): Boolean {
            return this.row == -1 && this.column == -1
        }
    }

}