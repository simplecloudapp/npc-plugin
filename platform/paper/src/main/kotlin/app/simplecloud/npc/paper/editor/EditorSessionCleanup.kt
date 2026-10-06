package app.simplecloud.npc.paper.editor

import app.simplecloud.npc.common.editor.ChatInputPrompts
import app.simplecloud.npc.common.editor.inventory.InventoryEditor
import app.simplecloud.npc.common.editor.npc.NpcEditor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class EditorSessionCleanup(
    private val chatPrompts: ChatInputPrompts,
    private val npcEditor: NpcEditor,
    private val inventoryEditor: InventoryEditor,
) : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        npcEditor.sessions.forgetPlayer(uuid)
        inventoryEditor.sessions.forgetPlayer(uuid)
        chatPrompts.cancel(uuid)
    }
}
