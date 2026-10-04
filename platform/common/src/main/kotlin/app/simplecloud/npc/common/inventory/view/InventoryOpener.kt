package app.simplecloud.npc.common.inventory.view

import app.simplecloud.npc.common.inventory.source.InventoryOpenContext
import app.simplecloud.npc.core.platform.NpcPlayer

fun interface InventoryOpener {
    fun open(player: NpcPlayer, inventoryId: String, context: InventoryOpenContext)

    fun back(player: NpcPlayer) = Unit
}
