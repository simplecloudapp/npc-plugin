package app.simplecloud.npc.common.platform

import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.core.platform.NpcPlayer
import java.util.UUID

data class RenderedInventory(
    val title: String,
    val size: Int,
    val slots: Map<Int, NpcItem>,
)

interface InventoryViewRenderer {
    fun runSync(task: () -> Unit)
    fun openView(player: NpcPlayer, viewId: UUID, view: RenderedInventory): Boolean
    fun updateSlots(viewId: UUID, changes: Map<Int, NpcItem?>)
    fun updateTitle(viewId: UUID, title: String)
    fun closeView(viewId: UUID)

    companion object {
        val NOOP: InventoryViewRenderer = object : InventoryViewRenderer {
            override fun runSync(task: () -> Unit) = task()
            override fun openView(player: NpcPlayer, viewId: UUID, view: RenderedInventory) = true
            override fun updateSlots(viewId: UUID, changes: Map<Int, NpcItem?>) = Unit
            override fun updateTitle(viewId: UUID, title: String) = Unit
            override fun closeView(viewId: UUID) = Unit
        }
    }
}
