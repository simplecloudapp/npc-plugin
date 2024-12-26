package app.simplecloud.npc.shared.inventory.transform

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig
import app.simplecloud.npc.shared.inventory.item.ItemCreator
import app.simplecloud.npc.shared.inventory.placeholder.InventoryPlaceholderHandler
import app.simplecloud.npc.shared.utils.PlayerConnectionHelper
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.grid.GridPoint
import com.noxcrew.interfaces.grid.GridPositionGenerator
import com.noxcrew.interfaces.interfaces.ChestInterfaceBuilder
import com.noxcrew.interfaces.pane.Pane
import com.noxcrew.interfaces.transform.builtin.PaginationButton
import com.noxcrew.interfaces.transform.builtin.PaginationTransformation

/**
 * @author Niklas Nieberler
 */

class PaginationTransformHandler(
    private val controllerApi: ControllerApi.Coroutine,
    private val placeholderHandler: InventoryPlaceholderHandler,
    config: InventoryConfig
) {

    private val pagination = config.pagination
        ?: throw NullPointerException("failed to find pagination configuration")
    private val itemCreator = ItemCreator(config)

    suspend fun handle(chestInterface: ChestInterfaceBuilder) {
        val reactiveTransform = PaginationTransformation<Pane>(
            buildGridPositionGenerator(),
            getListedGroupElements(),
            buildPaginationButton(this.pagination.previousPageItem),
            buildPaginationButton(this.pagination.nextPageItem)
        )
        chestInterface.addTransform(reactiveTransform)
    }

    private suspend fun getListedGroupElements(): List<StaticElement> {
        return this.controllerApi.getServers().getServersByGroup(this.pagination.listedGroupName)
            .filter { this.pagination.stateItems.contains(it.state) }
            .sortedBy { it.state.number }
            .map { buildListedGroupElement(it) }
    }

    private fun buildListedGroupElement(server: Server): StaticElement {
        val itemId = this.pagination.stateItems[server.state] ?: "default"
        val drawable = this.itemCreator.buildDrawableItem(itemId) { this.placeholderHandler.placeholderComponent(server, it) }
            ?: throw NullPointerException("failed to find item")
        return StaticElement(drawable) {
            val serverName = pagination.serverNamePattern
                .replace("<group_name>", server.group)
                .replace("<id>", server.uniqueId)
                .replace("<numerical_id>", server.numericalId.toString())
            PlayerConnectionHelper.sendPlayerToServer(it.player, serverName)
            it.player.closeInventory()
        }
    }

    private fun buildGridPositionGenerator(): GridPositionGenerator {
        val slots = getFromAndToSlotElements(this.pagination.fromSlot, this.pagination.toSlot)
            .filter { !this.pagination.excludedSlots.contains(it) }
            .map { GridPoint(it.row, it.column) }
        return GridPositionGenerator { slots }
    }

    private fun buildPaginationButton(item: InventoryConfig.PaginationInventory.PaginationItem): PaginationButton {
        val drawable = this.itemCreator.buildDrawableItem(item.item)
            ?: throw NullPointerException("failed to find item")
        val itemSlot = item.slot
        return PaginationButton(
            GridPoint(itemSlot.row, itemSlot.column),
            drawable,
            item.increments
        )
    }

    private fun getFromAndToSlotElements(
        fromSlot: InventoryConfig.ItemSlot,
        toSlot: InventoryConfig.ItemSlot
    ): List<InventoryConfig.ItemSlot> {
        if (fromSlot.isBlank() && toSlot.isBlank())
            return emptyList()
        var currentRow = fromSlot.row
        var currentColumn = fromSlot.column

        val list = arrayListOf<InventoryConfig.ItemSlot>()

        while (currentRow <= toSlot.row) {
            while (currentColumn < 9 && (currentRow < toSlot.row || currentColumn <= toSlot.column)) {
                list.add(InventoryConfig.ItemSlot(currentRow, currentColumn))
                currentColumn++
            }
            currentColumn = 0
            currentRow++
        }
        return list
    }

}