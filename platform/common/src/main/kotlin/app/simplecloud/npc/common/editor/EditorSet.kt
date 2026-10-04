package app.simplecloud.npc.common.editor

import app.simplecloud.npc.common.editor.inventory.InventoryEditor
import app.simplecloud.npc.common.editor.npc.NpcEditor
import app.simplecloud.npc.common.platform.Editors
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer

class EditorSet(
    private val npcEditor: NpcEditor,
    private val inventoryEditor: InventoryEditor,
) : Editors {
    override fun openNpc(player: NpcPlayer, config: NpcConfig) = npcEditor.open(player, config)
    override fun openInventory(player: NpcPlayer, config: InventoryConfiguration) = inventoryEditor.open(player, config)
    override fun openInventoryCreate(player: NpcPlayer) = inventoryEditor.openCreate(player)
    override fun refreshOpenEditors(players: List<NpcPlayer>) {
        npcEditor.refreshOpenScreens(players)
        inventoryEditor.refresh(players)
    }
}
