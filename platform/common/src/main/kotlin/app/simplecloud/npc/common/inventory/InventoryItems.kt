package app.simplecloud.npc.common.inventory

import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration

object InventoryItems {

    fun itemAt(config: InventoryConfiguration, slot: Int): InventoryItemConfiguration? =
        config.items.firstOrNull { it.slot == slot }

    fun removeItem(
        config: InventoryConfiguration,
        slot: Int,
    ): MutableList<InventoryItemConfiguration> {
        val existing = itemAt(config, slot) ?: return config.items.toMutableList()

        return handOverSource(existing, config.items.filterNot { it.slot == slot }.toMutableList())
    }

    fun detachFromGroup(
        config: InventoryConfiguration,
        slot: Int,
    ): MutableList<InventoryItemConfiguration> {
        val existing = itemAt(config, slot) ?: return config.items.toMutableList()
        val detached = existing.copy(liveGroup = null, live = null, whenEmpty = null, pageControl = null)

        return handOverSource(existing, config.items.map { if (it.slot == slot) detached else it }.toMutableList())
    }

    private fun handOverSource(
        leaving: InventoryItemConfiguration,
        items: MutableList<InventoryItemConfiguration>,
    ): MutableList<InventoryItemConfiguration> {
        if (leaving.liveGroup == null || (leaving.live == null && leaving.whenEmpty == null)) return items

        val nextIndex = items.indexOfFirst {
            it.slot != leaving.slot && it.liveGroup == leaving.liveGroup && it.pageControl == null
        }
        if (nextIndex >= 0) {
            items[nextIndex] = items[nextIndex].copy(
                live = items[nextIndex].live ?: leaving.live,
                whenEmpty = items[nextIndex].whenEmpty ?: leaving.whenEmpty,
            )
        }

        return items
    }
}
