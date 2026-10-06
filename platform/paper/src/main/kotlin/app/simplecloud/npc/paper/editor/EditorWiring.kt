package app.simplecloud.npc.paper.editor

import app.simplecloud.npc.common.editor.ChatInputPrompts
import app.simplecloud.npc.common.editor.EditorSet
import app.simplecloud.npc.common.editor.inventory.InventoryEditor
import app.simplecloud.npc.common.editor.npc.NpcEditor
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.paper.canvas.PacketCanvasSurface
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

class EditorWiring private constructor(
    val editors: EditorSet,
    val listeners: List<Any>,
) {

    companion object {
        private const val HUD_TICKS = 40L

        fun create(plugin: Plugin, context: () -> NpcPluginContext): EditorWiring {
            val chatPrompts = ChatInputPrompts()
            Bukkit.getScheduler().runTaskTimer(plugin, Runnable { chatPrompts.refreshHud() }, HUD_TICKS, HUD_TICKS)
            val menuOpener = PaperEditorMenuOpener(plugin)
            val canvasSurface = PacketCanvasSurface(plugin)

            val npcEditor = NpcEditor(menuOpener, chatPrompts, canvasSurface, pluginContextProvider = context)
            val inventoryEditor = InventoryEditor(menuOpener, chatPrompts, canvasSurface, context)

            return EditorWiring(
                EditorSet(npcEditor, inventoryEditor),
                listOf(
                    menuOpener,
                    canvasSurface,
                    EditorChatListener(chatPrompts, plugin),
                    EditorSessionCleanup(chatPrompts, npcEditor, inventoryEditor),
                ),
            )
        }
    }
}
