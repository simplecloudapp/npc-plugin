package app.simplecloud.npc.common.inventory.view

import app.simplecloud.npc.common.item.NpcItem

object InventoryLayout {

    fun maxPage(slotCount: Int, entryCount: Int): Int =
        ((entryCount - 1).coerceAtLeast(0)) / slotCount.coerceAtLeast(1)

    fun <T> pageWindow(items: List<T>, slotCount: Int, page: Int): List<T> =
        slotCount.coerceAtLeast(1).let { size -> items.drop(page * size).take(size) }

    fun diff(old: Map<Int, NpcItem>, new: Map<Int, NpcItem>): Map<Int, NpcItem?> = buildMap {
        new.forEach { (slot, icon) -> if (old[slot] != icon) put(slot, icon) }
        old.keys.forEach { slot -> if (slot !in new) put(slot, null) }
    }
}
