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
    data class RemoveJoinState(val joinState: String) : ConfirmPurpose
    data class RemoveLayout(val joinState: String) : ConfirmPurpose
}

enum class NpcTab(val label: String, val material: String, val description: String) {
    NPC("NPC", "PLAYER_HEAD", "Summon, copy or delete it"),
    LOOKS("Looks", "ARMOR_STAND", "Skin, name, glow, pose"),
    BEHAVIOR("Behavior", "PISTON", "Looking and pushing"),
    HOLOGRAM("Hologram", "GLOW_ITEM_FRAME", "Lines above its head"),
    ACTIONS("Actions", "LEVER", "What a click does"),
    TARGETS("Targets", "COMPASS", "Where it sends players"),
}

sealed class NpcEditorScreen(val npcId: String) {
    data class Tab(private val id: String, val tab: NpcTab) : NpcEditorScreen(id)
    data class SkinPicker(private val id: String) : NpcEditorScreen(id)
    data class Glow(private val id: String) : NpcEditorScreen(id)
    data class Equipment(private val id: String) : NpcEditorScreen(id)
    data class HologramFrames(private val id: String, val joinState: String, val line: Int) : NpcEditorScreen(id)
    data class ActionEditor(private val id: String, val interaction: PlayerInteraction, val joinState: String) :
        NpcEditorScreen(id)

    data class ActionBlocks(private val id: String, val interaction: PlayerInteraction, val joinState: String) :
        NpcEditorScreen(id)

    data class TitleEditor(private val id: String, val interaction: PlayerInteraction, val joinState: String) :
        NpcEditorScreen(id)

    data class Picker(private val id: String, val purpose: PickerPurpose) : NpcEditorScreen(id)
    data class Confirm(private val id: String, val purpose: ConfirmPurpose) : NpcEditorScreen(id)
}
