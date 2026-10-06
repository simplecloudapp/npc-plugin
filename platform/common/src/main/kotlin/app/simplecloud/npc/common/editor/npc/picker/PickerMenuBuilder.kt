package app.simplecloud.npc.common.editor.npc.picker

import app.simplecloud.npc.common.editor.core.PickerResult
import app.simplecloud.npc.common.editor.core.PickerSpec
import app.simplecloud.npc.common.editor.core.PickerTemplate
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

object PickerMenuBuilder {

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, purpose: PickerPurpose): EditorMenu {
        val source = PickerSource.of(purpose)
        val spec = PickerSpec<NpcEditorScreen>(
            title = Ui.title(source.title, NpcFormat.displayName(config)),
            screen = NpcEditorScreen.Picker(config.id, purpose),
            options = source.options(context, player, config),
            onPick = { value -> source.apply(context, player, config, value) },
            typeHint = source.typeHint,
            typingUnavailable = source.typingUnavailable,
            onTyped = { typed ->
                context.npcRepository.find(config.id)?.let { fresh ->
                    source.resolveTyped(context, fresh, typed).also { result ->
                        if (result == null) source.apply(context, player, fresh, typed)
                    }
                }
            },
            onRightClick = { option -> source.onRightClick(context, player, config, option) },
            extras = { pane -> source.extras(context, player, config, pane) },
            filteredNoun = source.filteredNoun,
        )

        return PickerTemplate.build(context, player, spec)
    }

    fun showResult(
        context: NpcEditorContext,
        player: NpcPlayer,
        config: NpcConfig,
        purpose: PickerPurpose,
        result: PickerResult,
    ) = PickerTemplate.showResult(context, player, NpcEditorScreen.Picker(config.id, purpose), result)
}
