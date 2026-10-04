package app.simplecloud.npc.paper.editor

import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.paper.inventory.AttachableInventoryHolder

class EditorMenuHolder(
    @Volatile var menu: EditorMenu,
    @Volatile var onClick: (slot: Int, click: MenuClick) -> Unit,
    @Volatile var onClose: () -> Unit,
) : AttachableInventoryHolder()
