package app.simplecloud.npc.common.editor.canvas

sealed interface CanvasInput {
    data class Click(val slot: Int, val right: Boolean, val shift: Boolean) : CanvasInput
    data class DoubleClick(val slot: Int) : CanvasInput
    data class Key(val slot: Int, val key: Int) : CanvasInput
    data class Drop(val slot: Int, val wholeStack: Boolean) : CanvasInput
    data class Drag(val slots: List<Int>, val kind: DragKind) : CanvasInput
    data class Outside(val right: Boolean) : CanvasInput
    data class Clone(val slot: Int) : CanvasInput

    companion object {
        const val OFFHAND_KEY = 40
    }
}

enum class DragKind { SPLIT, ONE_EACH, CLONE }
