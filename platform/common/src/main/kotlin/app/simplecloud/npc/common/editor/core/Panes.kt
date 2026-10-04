package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.menu.Paginator
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.platform.NpcPlayer

fun Pane.back(host: EditorHost<*>, player: NpcPlayer) =
    left(navRow.first, Ui.back()) { host.back(player) }

fun Pane.toggle(
    slot: Int,
    label: String,
    enabled: Boolean,
    extraLore: List<String> = emptyList(),
    material: String? = null,
    onClick: () -> Unit,
) = left(slot, Ui.toggle(label, enabled, extraLore, material), onClick)

fun pageKey(screen: Any): String = screen.toString()

fun Pane.pager(pages: Paginator<*>, lore: List<String>, rerender: () -> Unit) {
    val center = navRow.first + 4
    left(center - 1, Ui.previousPage(pages.page)) {
        pages.turn(-1)
        rerender()
    }
    this[center] = Ui.pageIndicator(pages.page, pages.pageCount, lore)
    left(center + 1, Ui.nextPage(pages.page, pages.pageCount)) {
        pages.turn(1)
        rerender()
    }
}
