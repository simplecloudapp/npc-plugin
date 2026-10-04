package app.simplecloud.npc.common.editor.menu

import app.simplecloud.npc.common.item.NpcItem

class Pane(val size: Int) {

    private val elements = mutableMapOf<Int, Element>()
    private var fallback: ((slot: Int, click: MenuClick) -> Unit)? = null

    val navRow: IntRange get() = (size - 9) until size

    operator fun set(slot: Int, item: NpcItem) {
        elements[slot] = Element(item)
    }

    operator fun set(slot: Int, element: Element) {
        elements[slot] = element
    }

    fun on(slot: Int, item: NpcItem, onClick: (MenuClick) -> Unit) {
        elements[slot] = Element(item, onClick)
    }

    fun on(slot: Int, item: NpcItem, click: MenuClick, action: () -> Unit) {
        elements[slot] = Element(item) { if (it == click) action() }
    }

    fun left(slot: Int, item: NpcItem, action: () -> Unit) = on(slot, item, MenuClick.LEFT, action)

    fun otherwise(handler: (slot: Int, click: MenuClick) -> Unit) {
        fallback = handler
    }

    fun fill(slots: Iterable<Int>, material: String = GRAY_PANE, onClick: ((MenuClick) -> Unit)? = null) {
        val pane = Element(filler(material), onClick ?: {})
        slots.forEach { slot -> elements.putIfAbsent(slot, pane) }
    }

    fun fillNavRow() = fill(navRow)

    fun menu(title: String): EditorMenu = EditorMenu(title, size, elements.toMap(), fallback)

    companion object {
        const val GRAY_PANE = "GRAY_STAINED_GLASS_PANE"
        const val RED_PANE = "RED_STAINED_GLASS_PANE"

        private fun filler(material: String): NpcItem = NpcItem(material, " ", hideTooltip = true)
    }
}
