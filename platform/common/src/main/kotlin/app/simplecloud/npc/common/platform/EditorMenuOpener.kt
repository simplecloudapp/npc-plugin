package app.simplecloud.npc.common.platform

import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.core.platform.NpcPlayer

interface EditorMenuOpener {
    fun runSync(task: () -> Unit)
    fun runLater(delayTicks: Long, task: () -> Unit)

    fun open(player: NpcPlayer, menu: EditorMenu, onClick: (slot: Int, click: MenuClick) -> Unit, onClose: () -> Unit)
    fun isShowing(player: NpcPlayer, menu: EditorMenu): Boolean

    fun detach(player: NpcPlayer) = Unit
}
