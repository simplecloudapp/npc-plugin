package app.simplecloud.npc.shared.inventory.configuration

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.config.NpcOption
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Niklas Nieberler
 */

@ConfigSerializable
data class InventoryConfig(
    val id: String = "",
    val pagination: PaginationInventory? = null,
    val staticSlots: List<StaticItem> = emptyList(),
    val items: List<InventoryItem> = emptyList()
) {

    @ConfigSerializable
    data class StaticItem(
        val item: String = "",
        val slot: ItemSlot? = ItemSlot(),
        val fromSlot: ItemSlot? = null,
        val toSlot: ItemSlot? = null
    )

    @ConfigSerializable
    data class PaginationInventory(
        val fromSlot: ItemSlot = ItemSlot(),
        val toSlot: ItemSlot = ItemSlot(),
        val nextPageItem: String = "",
        val previousPageItem: String = ""
    )

    @ConfigSerializable
    data class InventoryItem(
        val id: String = "",
        val material: String = "STONE",
        val displayName: String = "",
        val clickAction: Action = Action.RUN_COMMAND,
        val options: List<NpcOption> = emptyList()
    )

    @ConfigSerializable
    data class ItemSlot(
        val row: Int = 0,
        val column: Int = 0
    )

}