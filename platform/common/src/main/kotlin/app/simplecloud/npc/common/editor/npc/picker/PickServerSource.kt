package app.simplecloud.npc.common.editor.npc.picker

import app.simplecloud.npc.common.editor.core.PickerOption
import app.simplecloud.npc.common.editor.core.PickerOptions
import app.simplecloud.npc.common.editor.core.PickerResult
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

class PickServerSource(override val purpose: PickerPurpose.PickServer) : PickerSource {
    override val title = "Send To Server"

    override fun options(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): List<PickerOption> =
        PickerOptions.servers(
            context.liveData.servers(),
            config.findAction(purpose.interaction, purpose.joinState)?.sendToServer,
        )

    override fun resolveTyped(context: NpcEditorContext, config: NpcConfig, typed: String): PickerResult? =
        PickerOptions.match(context.liveData.servers().map { it.name }, typed, "No server by that name.")

    override fun apply(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, value: String) {
        val canonical = PickerOptions.canonical(context.liveData.servers().map { it.name }, value)
        context.commitAndBack(player, config.id) { fresh ->
            fresh.apply { actionOrCreate(purpose.interaction, purpose.joinState).sendToServer = canonical }
        }
    }
}
