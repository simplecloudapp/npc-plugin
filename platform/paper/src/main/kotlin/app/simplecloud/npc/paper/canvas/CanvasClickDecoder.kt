package app.simplecloud.npc.paper.canvas

import app.simplecloud.npc.common.editor.canvas.CanvasInput
import app.simplecloud.npc.common.editor.canvas.DragKind
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType

class CanvasClickDecoder {

    private var dragKind: DragKind? = null
    private val dragSlots = mutableListOf<Int>()

    fun decode(type: WindowClickType, button: Int, slot: Int): CanvasInput? {
        if (type != WindowClickType.QUICK_CRAFT) resetDrag()

        return when (type) {
            WindowClickType.PICKUP -> when (slot) {
                OUTSIDE -> CanvasInput.Outside(right = button == 1)
                in 0..Int.MAX_VALUE -> CanvasInput.Click(slot, right = button == 1, shift = false)
                else -> null
            }

            WindowClickType.QUICK_MOVE -> slotOrNull(slot)?.let {
                CanvasInput.Click(it, right = button == 1, shift = true)
            }
            WindowClickType.SWAP -> slotOrNull(slot)?.let { CanvasInput.Key(it, button) }
            WindowClickType.CLONE -> slotOrNull(slot)?.let { CanvasInput.Clone(it) }
            WindowClickType.THROW -> slotOrNull(slot)?.let { CanvasInput.Drop(it, wholeStack = button == 1) }
            WindowClickType.PICKUP_ALL -> slotOrNull(slot)?.let { CanvasInput.DoubleClick(it) }
            WindowClickType.QUICK_CRAFT -> drag(button, slot)
            WindowClickType.UNKNOWN -> null
        }
    }

    private fun drag(button: Int, slot: Int): CanvasInput? {
        val kind = DRAG_KINDS.getOrNull((button shr 2) and 3)
        when (button and 3) {
            STAGE_START -> {
                resetDrag()
                dragKind = kind
            }

            STAGE_ADD -> if (kind != null && kind == dragKind && slot >= 0 && slot !in dragSlots) dragSlots += slot

            STAGE_END -> {
                val started = dragKind
                val slots = dragSlots.toList()
                resetDrag()
                if (started != null && started == kind && slots.isNotEmpty()) return CanvasInput.Drag(slots, started)
            }
        }

        return null
    }

    private fun resetDrag() {
        dragKind = null
        dragSlots.clear()
    }

    private fun slotOrNull(slot: Int): Int? = slot.takeIf { it >= 0 }

    private companion object {
        const val OUTSIDE = -999
        const val STAGE_START = 0
        const val STAGE_ADD = 1
        const val STAGE_END = 2
        val DRAG_KINDS = listOf(DragKind.SPLIT, DragKind.ONE_EACH, DragKind.CLONE)
    }
}
