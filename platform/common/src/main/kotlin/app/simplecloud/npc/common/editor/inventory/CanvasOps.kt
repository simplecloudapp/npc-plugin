package app.simplecloud.npc.common.editor.inventory

import app.simplecloud.npc.common.inventory.InventoryItems
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration

object CanvasOps {

    private const val WIDTH = 9

    fun itemAt(config: InventoryConfiguration, slot: Int): InventoryItemConfiguration? =
        InventoryItems.itemAt(config, slot)

    fun put(config: InventoryConfiguration, slot: Int, item: InventoryItemConfiguration): InventoryConfiguration? {
        if (slot !in 0 until config.size()) return null
        val cleared = config.copy(items = InventoryItems.removeItem(config, slot))

        return cleared.copy(items = (cleared.items + asCopyFor(cleared, item, slot)).toMutableList())
    }

    fun remove(config: InventoryConfiguration, slots: Collection<Int>): InventoryConfiguration? {
        val present = slots.filter { itemAt(config, it) != null }.ifEmpty { return null }
        return present.fold(config) { current, slot -> current.copy(items = InventoryItems.removeItem(current, slot)) }
    }

    fun move(config: InventoryConfiguration, from: Int, to: Int): InventoryConfiguration? {
        if (from == to || to !in 0 until config.size()) return null
        val moving = itemAt(config, from) ?: return null
        val other = itemAt(config, to)

        val items = config.items.mapNotNull {
            when (it.slot) {
                from -> other?.copy(slot = from)
                to -> null
                else -> it
            }
        }

        return config.copy(items = (items + moving.copy(slot = to)).toMutableList())
    }

    fun stamp(
        config: InventoryConfiguration,
        slots: Collection<Int>,
        item: InventoryItemConfiguration,
    ): InventoryConfiguration? {
        val targets = slots.filter { it in 0 until config.size() }.distinct().ifEmpty { return null }
        return targets.fold(config) { current, slot -> put(current, slot, item) ?: current }
    }

    fun addRow(config: InventoryConfiguration): InventoryConfiguration? =
        if (config.rows >= InventoryConfiguration.MAX_ROWS) null else config.copy(rows = config.rows + 1)

    fun removeRow(config: InventoryConfiguration): InventoryConfiguration? {
        if (config.rows <= InventoryConfiguration.MIN_ROWS) return null
        val newSize = (config.rows - 1) * WIDTH
        if (config.items.any { it.slot >= newSize }) return null

        return config.copy(rows = config.rows - 1)
    }

    fun shift(
        config: InventoryConfiguration,
        rows: Int,
        columns: Int,
        slots: Set<Int>? = null,
    ): InventoryConfiguration? {
        if (rows == 0 && columns == 0) return null
        val (moving, kept) = config.items.partition { slots == null || it.slot in slots }
        if (moving.isEmpty()) return null
        val staying = kept.mapTo(mutableSetOf()) { it.slot }

        val moved = moving.map { item ->
            val row = item.slot / WIDTH + rows
            val column = item.slot % WIDTH + columns
            if (row !in 0 until config.rows || column !in 0 until WIDTH) return null
            val target = row * WIDTH + column
            if (target in staying) return null
            item.copy(slot = target)
        }

        return config.copy(items = (config.items.filter { it.slot in staying } + moved).toMutableList())
    }

    fun similar(config: InventoryConfiguration, slot: Int): Set<Int> {
        val reference = itemAt(config, slot) ?: return emptySet()
        return config.items.filter { sameLook(it, reference) }.map { it.slot }.toSet()
    }

    fun sameLook(a: InventoryItemConfiguration, b: InventoryItemConfiguration): Boolean =
        a.material == b.material && a.name == b.name && a.lore == b.lore && a.amount == b.amount &&
            a.glowing == b.glowing && a.customModelData == b.customModelData && a.liveGroup == b.liveGroup &&
            a.pageControl == b.pageControl

    private fun asCopyFor(
        config: InventoryConfiguration,
        item: InventoryItemConfiguration,
        slot: Int,
    ): InventoryItemConfiguration {
        val copy = item.deepCopy().copy(slot = slot)
        val group = copy.liveGroup ?: return copy
        val groupHasSource = config.items.any { it.liveGroup == group && (it.live != null || it.whenEmpty != null) }

        return if (groupHasSource) copy.copy(live = null, whenEmpty = null) else copy
    }
}
