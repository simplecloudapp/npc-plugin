package app.simplecloud.npc.common.editor.inventory

import app.simplecloud.npc.core.inventory.InventoryConfiguration

class EditHistory(private val limit: Int = DEFAULT_LIMIT) {

    private val undo = ArrayDeque<InventoryConfiguration>()
    private val redo = ArrayDeque<InventoryConfiguration>()

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()
    val undoCount: Int get() = undo.size
    val redoCount: Int get() = redo.size

    fun record(before: InventoryConfiguration) {
        push(undo, before.deepCopy())
        redo.clear()
    }

    fun undo(current: InventoryConfiguration): InventoryConfiguration? =
        undo.removeLastOrNull()?.also { push(redo, current.deepCopy()) }

    fun redo(current: InventoryConfiguration): InventoryConfiguration? =
        redo.removeLastOrNull()?.also { push(undo, current.deepCopy()) }

    private fun push(stack: ArrayDeque<InventoryConfiguration>, config: InventoryConfiguration) {
        stack.addLast(config)
        while (stack.size > limit) stack.removeFirst()
    }

    companion object {
        const val DEFAULT_LIMIT = 50
    }
}
