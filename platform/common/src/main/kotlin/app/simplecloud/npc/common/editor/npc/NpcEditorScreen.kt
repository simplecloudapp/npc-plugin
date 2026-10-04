package app.simplecloud.npc.common.editor.npc

import app.simplecloud.npc.core.interaction.PlayerInteraction

sealed interface PickerPurpose {
    data object AddTarget : PickerPurpose

    data class AddJoinState(val forHologram: Boolean) : PickerPurpose
    data class PickMenu(val interaction: PlayerInteraction, val joinState: String) : PickerPurpose
    data class PickServer(val interaction: PlayerInteraction, val joinState: String) : PickerPurpose
    data class PickSound(val field: SoundField) : PickerPurpose
}

sealed interface SoundField {
    data object Push : SoundField

    data class Action(val interaction: PlayerInteraction, val joinState: String) : SoundField
}

sealed interface ConfirmPurpose {
    data object DeleteNpc : ConfirmPurpose

    data class WipeAction(val interaction: PlayerInteraction, val joinState: String) : ConfirmPurpose
    data class RemoveJoinState(val joinState: String) : ConfirmPurpose
    data class RemoveLayout(val joinState: String) : ConfirmPurpose
}

sealed class NpcEditorScreen(val npcId: String) {
    data class Hub(private val id: String) : NpcEditorScreen(id)
    data class Appearance(private val id: String) : NpcEditorScreen(id)
    data class SkinPicker(private val id: String) : NpcEditorScreen(id)
    data class Glow(private val id: String) : NpcEditorScreen(id)
    data class Equipment(private val id: String) : NpcEditorScreen(id)
    data class Behavior(private val id: String) : NpcEditorScreen(id)
    data class Targets(private val id: String) : NpcEditorScreen(id)
    data class Hologram(private val id: String) : NpcEditorScreen(id)
    data class HologramLines(private val id: String, val joinState: String) : NpcEditorScreen(id)
    data class HologramFrames(private val id: String, val joinState: String, val line: Int) : NpcEditorScreen(id)
    data class ActionMatrix(private val id: String) : NpcEditorScreen(id)
    data class ActionEditor(private val id: String, val interaction: PlayerInteraction, val joinState: String) :
        NpcEditorScreen(id)

    data class TitleEditor(private val id: String, val interaction: PlayerInteraction, val joinState: String) :
        NpcEditorScreen(id)

    data class Picker(private val id: String, val purpose: PickerPurpose) : NpcEditorScreen(id)
    data class Confirm(private val id: String, val purpose: ConfirmPurpose) : NpcEditorScreen(id)
}
