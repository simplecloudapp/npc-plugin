package app.simplecloud.npc.common.editor.npc.action

import app.simplecloud.npc.common.editor.core.ActionFieldsPane
import app.simplecloud.npc.common.editor.core.armed
import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.npc.SoundField
import app.simplecloud.npc.common.editor.ui.Clicks
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.platform.NpcPlayer

object ActionEditorMenuBuilder {

    fun build(
        context: NpcEditorContext,
        player: NpcPlayer,
        config: NpcConfig,
        interaction: PlayerInteraction,
        joinState: String,
    ): EditorMenu {
        val screen = NpcEditorScreen.ActionEditor(config.id, interaction, joinState)
        val pane = Pane(ActionFieldsPane.SIZE)

        pane[ActionFieldsPane.HEADER_SLOT] = Ui.item(
            Clicks.material(interaction),
            "<ttl>${Clicks.label(interaction)}",
            listOf("<hnt>Join state <val>${NpcFormat.joinState(joinState)}"),
            glowing = true,
        )

        ActionFieldsPane.fill(pane, port(context, player, config, interaction, joinState, screen))

        pane.back(context, player)
        if (config.findAction(interaction, joinState) != null) {
            pane.armed(
                ActionFieldsPane.DELETE_SLOT,
                context,
                player,
                screen,
                "delete-action",
                idle = Ui.item("RED_CONCRETE", "<err>Delete Action", listOf("<key>Left <hnt>Remove everything")),
                verb = "Delete this action",
            ) {
                context.commitAndBack(player, config.id) { fresh ->
                    fresh.actions.removeIf { it.interactionType == interaction && it.joinState.equals(joinState, true) }
                    fresh
                }
            }
        }
        pane.fillNavRow()

        return pane.menu(Ui.title(NpcFormat.cellName(interaction, joinState), NpcFormat.displayName(config)))
    }

    fun buildBlocks(
        context: NpcEditorContext,
        player: NpcPlayer,
        config: NpcConfig,
        interaction: PlayerInteraction,
        joinState: String,
    ): EditorMenu {
        val editor = NpcEditorScreen.ActionEditor(config.id, interaction, joinState)
        val pane = Pane(ActionFieldsPane.ADD_SIZE)

        ActionFieldsPane.fillAdd(pane, port(context, player, config, interaction, joinState, editor))
        pane.back(context, player)
        pane.fillNavRow()

        return pane.menu(Ui.title("Add to ${Clicks.short(interaction)}", NpcFormat.displayName(config)))
    }

    private fun port(
        context: NpcEditorContext,
        player: NpcPlayer,
        config: NpcConfig,
        interaction: PlayerInteraction,
        joinState: String,
        screen: NpcEditorScreen.ActionEditor,
    ): ActionFieldsPane.Port {
        fun navigate(purpose: PickerPurpose) = context.navigate(player, NpcEditorScreen.Picker(config.id, purpose))

        return ActionFieldsPane.Port(
            scope = ActionFieldsPane.Scope.NPC,
            player = player,
            action = config.findAction(interaction, joinState) ?: NpcConfig.ActionConfiguration(interaction, joinState),
            edit = { mutate ->
                context.commit(player, config.id, screen) { fresh ->
                    fresh.apply {
                        mutate(actionOrCreate(interaction, joinState))
                        dropActionIfEmpty(interaction, joinState)
                    }
                }
            },
            toggleJoinTarget = {
                context.commit(player, config.id, screen) { fresh ->
                    val target = fresh.actionOrCreate(interaction, joinState)
                    val enable = !target.joinTarget
                    if (enable) fresh.actions.forEach { it.joinTarget = false }
                    target.joinTarget = enable
                    fresh.dropActionIfEmpty(interaction, joinState)
                    fresh
                }
            },
            prompt = { context.chatPrompt(player, it) },
            rerender = { context.render(player, screen) },
            pickMenu = { navigate(PickerPurpose.PickMenu(interaction, joinState)) },
            pickServer = { navigate(PickerPurpose.PickServer(interaction, joinState)) },
            pickSound = { navigate(PickerPurpose.PickSound(SoundField.Action(interaction, joinState))) },
            openTitle = { context.navigate(player, NpcEditorScreen.TitleEditor(config.id, interaction, joinState)) },
            openAdd = { context.navigate(player, NpcEditorScreen.ActionBlocks(config.id, interaction, joinState)) },
            closeAdd = { context.back(player) },
        )
    }
}
