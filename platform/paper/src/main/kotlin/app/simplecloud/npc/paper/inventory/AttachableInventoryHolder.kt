package app.simplecloud.npc.paper.inventory

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

open class AttachableInventoryHolder : InventoryHolder {
    private var backing: Inventory? = null

    override fun getInventory(): Inventory = backing ?: error("Inventory not yet attached")

    fun attach(inventory: Inventory) {
        backing = inventory
    }
}
