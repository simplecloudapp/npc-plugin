package app.simplecloud.npc.common.editor.npc.picker

import app.simplecloud.npc.common.editor.core.PickerOption
import app.simplecloud.npc.common.editor.core.PickerResult
import app.simplecloud.npc.common.editor.core.PickerSpec
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

interface PickerSource {
    val purpose: PickerPurpose
    val title: String

    val typeHint: List<String>? get() = PickerSpec.DEFAULT_TYPE_HINT

    val filteredNoun: String get() = "options"

    val typingUnavailable: String get() = "Pick one from the list above."

    fun options(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): List<PickerOption>

    fun resolveTyped(context: NpcEditorContext, config: NpcConfig, typed: String): PickerResult? = null

    fun apply(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, value: String)

    fun onRightClick(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, option: PickerOption) = Unit

    fun extras(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, pane: Pane) = Unit

    companion object {
        fun of(purpose: PickerPurpose): PickerSource = when (purpose) {
            PickerPurpose.AddTarget -> AddTargetSource
            is PickerPurpose.AddJoinState -> AddJoinStateSource(purpose)
            is PickerPurpose.PickMenu -> PickMenuSource(purpose)
            is PickerPurpose.PickServer -> PickServerSource(purpose)
            is PickerPurpose.PickSound -> PickSoundSource(purpose)
        }
    }
}
