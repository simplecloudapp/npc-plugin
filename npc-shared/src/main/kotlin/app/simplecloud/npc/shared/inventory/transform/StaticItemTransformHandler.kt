package app.simplecloud.npc.shared.inventory.transform

import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig
import app.simplecloud.npc.shared.inventory.item.ItemCreator
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.ChestInterfaceBuilder
import com.noxcrew.interfaces.pane.Pane

/**
 * @author Niklas Nieberler
 */

class StaticItemTransformHandler(
    private val config: InventoryConfig
) {

    private val itemCreator = ItemCreator(config)

    fun handle(chestInterface: ChestInterfaceBuilder) {
        chestInterface.withTransform { pane, view ->
            this.config.staticSlots.forEach { buildStaticItemToPane(pane, it) }
        }
    }

    private fun buildStaticItemToPane(pane: Pane, item: InventoryConfig.StaticItem) {
        val slots = item.slots
        if (slots.isEmpty()) {
            setFromAndToElements(pane, item)
            return
        }
        slots.forEach { setStaticElement(pane, it, item) }
    }

    private fun setStaticElement(pane: Pane, slot: InventoryConfig.ItemSlot, item: InventoryConfig.StaticItem) {
        val drawable = this.itemCreator.buildDrawableItem(item.item)
            ?: throw NullPointerException("failed to find item")
        pane[slot.row, slot.column] = StaticElement(drawable)
    }

    private fun setFromAndToElements(pane: Pane, item: InventoryConfig.StaticItem) {
        val fromSlot = item.fromSlot ?: return
        val toSlot = item.toSlot ?: return
        if (fromSlot.isBlank() && toSlot.isBlank())
            return

        var currentRow = fromSlot.row
        var currentColumn = fromSlot.column

        while (currentRow <= toSlot.row) {
            while (currentColumn < 9 && (currentRow < toSlot.row || currentColumn <= toSlot.column)) {
                setStaticElement(pane, InventoryConfig.ItemSlot(currentRow, currentColumn), item)
                currentColumn++
            }
            currentColumn = 0
            currentRow++
        }
    }

}