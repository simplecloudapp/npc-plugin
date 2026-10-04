package app.simplecloud.npc.common.editor.npc.action

import app.simplecloud.npc.common.editor.core.TitlePane
import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.platform.NpcPlayer

object TitleEditorMenuBuilder {

    fun build(
        context: NpcEditorContext,
        player: NpcPlayer,
        config: NpcConfig,
        interaction: PlayerInteraction,
        joinState: String,
    ): EditorMenu {
        val screen = NpcEditorScreen.TitleEditor(config.id, interaction, joinState)
        val pane = Pane(TitlePane.SIZE)

        TitlePane.fill(
            pane,
            TitlePane.Port(
                player = player,
                title = config.findAction(interaction, joinState)?.sendTitle,
                textPrompts = context.textPrompts,
                edit = { mutate ->
                    context.commit(player, config.id, screen) { fresh ->
                        val action = fresh.actionOrCreate(interaction, joinState)
                        action.sendTitle = TitlePane.normalized(mutate(action.sendTitle ?: TitlePane.defaults()))
                        fresh.dropActionIfEmpty(interaction, joinState)
                        fresh
                    }
                },
                removeAndBack = {
                    context.commitAndBack(player, config.id) { fresh ->
                        fresh.apply {
                            actionOrCreate(interaction, joinState).sendTitle = null
                            dropActionIfEmpty(interaction, joinState)
                        }
                    }
                },
            ),
        )

        pane.back(context, player)
        pane.fillNavRow()

        return pane.menu(Ui.title("Title", NpcFormat.displayName(config)))
    }
}
