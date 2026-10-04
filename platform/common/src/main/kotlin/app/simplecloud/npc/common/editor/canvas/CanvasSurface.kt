package app.simplecloud.npc.common.editor.canvas

import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer

data class CanvasView(
    val title: String,
    val size: Int,
    val items: Map<Int, NpcItem>,
    val toolbox: Map<Int, NpcItem> = emptyMap(),
    val carried: NpcItem? = null,
    val showInventory: Boolean = false,
) {
    companion object {
        const val TOOLBOX_SIZE = 36
    }
}

interface CanvasSurface {
    fun open(player: NpcPlayer, view: CanvasView, onInput: (CanvasInput) -> Unit, onClose: () -> Unit)
    fun detach(player: NpcPlayer)
    fun inventoryItem(player: NpcPlayer, index: Int): InventoryItemConfiguration?
    fun equipmentItem(player: NpcPlayer, index: Int): NpcConfig.EquipmentItem?
}
