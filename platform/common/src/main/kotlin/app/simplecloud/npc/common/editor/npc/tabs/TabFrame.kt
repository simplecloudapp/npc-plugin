package app.simplecloud.npc.common.editor.npc.tabs

import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.NpcTab
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

object TabFrame {
    const val SIZE = 54

    private const val EXIT_SLOT = 8
    private const val UNDERLINE_FIRST = 9
    private val HEADER = 0..17
    private val FOOTER = 45..53

    private const val ACTIVE_LINE = "LIGHT_BLUE_STAINED_GLASS_PANE"

    fun pane(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, active: NpcTab): Pane {
        val pane = Pane(SIZE)

        NpcTab.entries.forEachIndexed { slot, tab ->
            val selected = tab == active
            val name = if (selected) "<on><b>${tab.label}" else "<ttl>${tab.label}"
            val lore = listOf("<hnt>${tab.description}") + if (selected) emptyList() else listOf("<key>Left <hnt>Open")
            val item = if (tab == NpcTab.NPC) {
                Ui.head(name, lore, config.entity.skin.texture, config.entity.skin.signature, glowing = selected)
            } else {
                Ui.item(tab.material, name, lore, glowing = selected)
            }

            if (selected) {
                pane[slot] = item
            } else {
                pane.left(slot, item) { context.replace(player, NpcEditorScreen.Tab(config.id, tab)) }
            }
        }

        pane.fill(listOf(UNDERLINE_FIRST + active.ordinal), ACTIVE_LINE)
        pane.left(EXIT_SLOT, Ui.close()) { context.close(player) }

        return pane
    }

    fun menu(pane: Pane, config: NpcConfig): EditorMenu {
        pane.fill(HEADER)
        pane.fill(FOOTER)

        return pane.menu(Ui.title("⚡ NPC Editor", NpcFormat.displayName(config)))
    }
}
