package app.simplecloud.npc.common.editor.inventory

import app.simplecloud.npc.core.interaction.PlayerInteraction

enum class StudioTab { LOOK, CLICK, LIVE }

sealed interface InventoryPickerPurpose {
    data object NewItem : InventoryPickerPurpose

    data class Material(val slots: List<Int>) : InventoryPickerPurpose

    data object NewLiveGroup : InventoryPickerPurpose

    data class LiveSourceType(val group: String) : InventoryPickerPurpose
    data class LiveSourceGroup(val group: String) : InventoryPickerPurpose
    data class LookMaterial(val group: String, val state: String) : InventoryPickerPurpose
    data object UsedBy : InventoryPickerPurpose
    data class OpenInventory(val slots: List<Int>, val interaction: PlayerInteraction) : InventoryPickerPurpose
    data class SendToServer(val slots: List<Int>, val interaction: PlayerInteraction) : InventoryPickerPurpose
    data class Sound(val slots: List<Int>, val interaction: PlayerInteraction) : InventoryPickerPurpose
}

sealed interface InventoryConfirmPurpose {
    data object DeleteInventory : InventoryConfirmPurpose
    data class DeleteLiveGroup(val group: String) : InventoryConfirmPurpose
}

sealed interface InventoryEditorScreen {

    data object Templates : InventoryEditorScreen

    sealed interface Bound : InventoryEditorScreen {
        val inventoryId: String
    }

    data class Hub(override val inventoryId: String) : Bound
    data class Canvas(override val inventoryId: String) : Bound

    data class Studio(
        override val inventoryId: String,
        val slots: List<Int>,
        val tab: StudioTab = StudioTab.LOOK,
    ) : Bound

    data class Lore(override val inventoryId: String, val slots: List<Int>) : Bound

    data class ItemAction(
        override val inventoryId: String,
        val slots: List<Int>,
        val interaction: PlayerInteraction,
    ) : Bound

    data class ItemTitle(
        override val inventoryId: String,
        val slots: List<Int>,
        val interaction: PlayerInteraction,
    ) : Bound

    data class LiveGroup(override val inventoryId: String, val group: String) : Bound
    data class StateLooks(override val inventoryId: String, val group: String) : Bound
    data class Picker(override val inventoryId: String, val purpose: InventoryPickerPurpose) : Bound
    data class Confirm(override val inventoryId: String, val purpose: InventoryConfirmPurpose) : Bound
}
