package app.simplecloud.npc.common.editor.inventory

import app.simplecloud.npc.common.editor.core.EditorSession
import app.simplecloud.npc.common.editor.core.EditorSessions
import app.simplecloud.npc.common.editor.inventory.canvas.CanvasRender
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration

enum class CanvasMode { DESIGN, LIVE }

data class Carried(val item: InventoryItemConfiguration, val from: Int?)

class InventoryEditorSession : EditorSession<InventoryEditorScreen>() {

    private val histories = mutableMapOf<String, EditHistory>()

    fun history(inventoryId: String): EditHistory = histories.getOrPut(inventoryId.lowercase()) { EditHistory() }

    var carried: Carried? = null
    var lastPlaced: InventoryItemConfiguration? = null
    var mode = CanvasMode.DESIGN

    var thisSlotOnly = false

    var activeGroup: String? = null

    var handMode = false

    var live: CanvasRender.LiveState? = null
    var liveSerial = 0
    val livePages = mutableMapOf<String, Int>()

    var lastCanvas: InventoryConfiguration? = null

    val selection = linkedSetOf<Int>()
    var selectionOf: String? = null
        private set

    fun selectionFor(inventoryId: String): Set<Int> =
        if (selectionOf.equals(inventoryId, true)) selection else emptySet()

    fun select(inventoryId: String, slots: Collection<Int>, add: Boolean = true) {
        if (!selectionOf.equals(inventoryId, true)) {
            selection.clear()
            selectionOf = inventoryId
        }
        if (add) selection += slots else selection -= slots.toSet()
    }

    fun clearSelection() {
        selection.clear()
        selectionOf = null
    }

    private val recent = ArrayDeque<String>()
    val recentMaterials: List<String> get() = recent.toList()

    fun usedMaterial(material: String) {
        recent.remove(material)
        recent.addFirst(material)
        while (recent.size > RECENT_MATERIALS) recent.removeLast()
    }

    var materialCategory: String = RECENT_CATEGORY

    val keyBindings = mutableMapOf<Int, InventoryItemConfiguration>()

    fun renameGroup(from: String, to: String) {
        fun moved(item: InventoryItemConfiguration) = if (item.liveGroup == from) item.copy(liveGroup = to) else item
        if (activeGroup == from) activeGroup = to
        lastPlaced = lastPlaced?.let(::moved)
        carried = carried?.let { it.copy(item = moved(it.item)) }
        keyBindings.replaceAll { _, item -> moved(item) }
    }

    fun leaveCanvas() {
        carried = null
        clearSelection()
        mode = CanvasMode.DESIGN
        handMode = false
        live = null
        livePages.clear()
    }

    companion object {
        const val RECENT_MATERIALS = 7
        const val RECENT_CATEGORY = "Recent"
        const val ALL_CATEGORY = "All"
    }
}

class InventoryEditorSessions : EditorSessions<InventoryEditorSession>(::InventoryEditorSession)
