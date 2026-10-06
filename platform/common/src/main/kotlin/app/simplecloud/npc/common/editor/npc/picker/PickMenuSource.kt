package app.simplecloud.npc.common.editor.npc.picker

import app.simplecloud.npc.common.editor.core.PickerOption
import app.simplecloud.npc.common.editor.core.PickerOptions
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

class PickMenuSource(override val purpose: PickerPurpose.PickMenu) : PickerSource {
    override val title = "Open Menu"
    override val typeHint: List<String>? = null
    override val typingUnavailable = "Menus are created in the Menu Builder."

    override fun options(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): List<PickerOption> =
        PickerOptions.menus(
            context.inventoryRepository.findAll(),
            config.findAction(purpose.interaction, purpose.joinState)?.openInventory,
        )

    override fun apply(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, value: String) {
        context.commitAndBack(player, config.id) { fresh ->
            fresh.apply { actionOrCreate(purpose.interaction, purpose.joinState).openInventory = value }
        }
    }
}
