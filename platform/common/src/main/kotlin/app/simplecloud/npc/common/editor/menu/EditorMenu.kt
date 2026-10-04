package app.simplecloud.npc.common.editor.menu

import app.simplecloud.npc.common.item.NpcItem

enum class MenuClick {
    LEFT,
    SHIFT_LEFT,
    RIGHT,
    SHIFT_RIGHT,
    DROP,
    MIDDLE,
    OFFHAND,
    DOUBLE_CLICK,
    OTHER;

    fun step(small: Double, big: Double): Double = when (this) {
        LEFT -> small
        SHIFT_LEFT -> big
        RIGHT -> -small
        SHIFT_RIGHT -> -big
        else -> 0.0
    }
}

class Element(
    val item: NpcItem,
    val onClick: (MenuClick) -> Unit = {},
)

class EditorMenu(
    val title: String,
    val size: Int,
    val elements: Map<Int, Element>,
    val fallback: ((slot: Int, click: MenuClick) -> Unit)? = null,
) {
    val items: Map<Int, NpcItem> by lazy { elements.mapValues { it.value.item } }

    fun click(slot: Int, click: MenuClick) {
        elements[slot]?.onClick?.invoke(click) ?: fallback?.invoke(slot, click)
    }
}
