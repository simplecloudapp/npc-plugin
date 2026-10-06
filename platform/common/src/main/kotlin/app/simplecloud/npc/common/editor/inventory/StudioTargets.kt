package app.simplecloud.npc.common.editor.inventory

import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration

object StudioTargets {

    fun of(config: InventoryConfiguration, slots: List<Int>, thisSlotOnly: Boolean): Set<Int> {
        val chosen = slots.filter { slot -> config.items.any { it.slot == slot } }.toSet()
        if (thisSlotOnly) return chosen
        val groups = config.items
            .filter { it.slot in chosen && it.pageControl == null }
            .mapNotNull { it.liveGroup }
            .toSet()

        return chosen + config.items.filter { it.liveGroup in groups && it.pageControl == null }.map { it.slot }
    }

    fun items(config: InventoryConfiguration, targets: Set<Int>): List<InventoryItemConfiguration> =
        config.items.filter { it.slot in targets }.sortedBy { it.slot }

    fun edit(
        config: InventoryConfiguration,
        targets: Set<Int>,
        transform: (InventoryItemConfiguration) -> InventoryItemConfiguration,
    ): InventoryConfiguration {
        val items = config.items.map { if (it.slot in targets) transform(it) else it }
        return config.copy(items = items.toMutableList())
    }

    fun <T> shared(items: List<InventoryItemConfiguration>, field: (InventoryItemConfiguration) -> T): Shared<T> {
        val values = items.map(field).distinct()
        return if (values.size == 1) Shared.Same(values.single()) else Shared.Mixed
    }

    sealed interface Shared<out T> {
        data class Same<T>(val value: T) : Shared<T>
        data object Mixed : Shared<Nothing>
    }
}
