package app.simplecloud.npc.shared.inventory

import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig
import app.simplecloud.npc.shared.inventory.item.ItemCreator
import app.simplecloud.npc.shared.text
import com.noxcrew.interfaces.drawable.Drawable
import com.noxcrew.interfaces.element.StaticElement
import com.noxcrew.interfaces.interfaces.CombinedInterfaceBuilder
import com.noxcrew.interfaces.interfaces.buildChestInterface
import com.noxcrew.interfaces.pane.Pane
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class NpcInventory(
    private val config: InventoryConfig
) {

    private val itemCreator = ItemCreator(config)

    private val inventory = createCombinedInterface()

    suspend fun open(player: Player) {
        val inventory = createCombinedInterface()
            inventory.open(player)

    }

    private fun createCombinedInterface() = buildChestInterface {
        println("createCombinedInterface")
        rows = 6
        initialTitle = text("YEEPEEE")
        try {
           // buildStaticItems(this)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun buildStaticItems(combinedInterfaceBuilder: CombinedInterfaceBuilder) {
        println("buildStaticItems")
        combinedInterfaceBuilder.withTransform { pane, view ->
            println("buildStaticItems 2")
            /*
            try {
                this.config.staticSlots.forEach {
                    println("static: $it")
                    buildStaticItem(pane, it)
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }

             */
        }
    }

    private fun buildStaticItem(pane: Pane, item: InventoryConfig.StaticItem) {
        try {
            println("yeehe ${item.item}")
            val drawable = this.itemCreator.buildDrawableItem(item.item) ?: throw NullPointerException("failed to find item")

            println("first")

            val slot = item.slot
            if (slot.isBlank()) {
                println("from and to slot")
                getGridPoint(item, pane, drawable)
                return
            }

            println("solo slot")
            pane[slot.row, slot.column] = StaticElement(drawable) {
                it.player.sendMessage("clicked")
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    private fun getGridPoint(item: InventoryConfig.StaticItem, pane: Pane, drawable: Drawable) {
        val fromSlot = item.fromSlot
        val toSlot = item.toSlot
        println("abdas ding 1")
        if (fromSlot.isBlank() && toSlot.isBlank())
            return

        println("abdas ding 2")

        val maxColumns = 9 // Maximal 9 Spalten pro Zeile

        var currentRow = fromSlot.row
        var currentColumn = fromSlot.column

        while (currentRow <= toSlot.row) {
            while (currentColumn < maxColumns && (currentRow < toSlot.row || currentColumn <= toSlot.column)) {
                pane[currentRow, currentColumn] = StaticElement(drawable) {
                    it.player.sendMessage("clicked")
                }
                currentColumn++
            }
            currentColumn = 0 // Zurück zur ersten Spalte für die nächste Zeile
            currentRow++
        }
    }

}