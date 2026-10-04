package app.simplecloud.npc.common.editor.npc.action

import app.simplecloud.npc.common.editor.core.ActionFieldsPane
import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.ConfirmPurpose
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

    private val FIELD_COUNT = ActionFieldsPane.fieldCount(ActionFieldsPane.Scope.NPC)

    fun build(
        context: NpcEditorContext,
        player: NpcPlayer,
        config: NpcConfig,
        interaction: PlayerInteraction,
        joinState: String,
    ): EditorMenu {
        val existing = config.findAction(interaction, joinState)
        val action = existing ?: NpcConfig.ActionConfiguration(interaction, joinState)
        val screen = NpcEditorScreen.ActionEditor(config.id, interaction, joinState)
        val pane = Pane(ActionFieldsPane.SIZE)

        fun navigate(purpose: PickerPurpose) = context.navigate(player, NpcEditorScreen.Picker(config.id, purpose))

        pane[ActionFieldsPane.HEADER_SLOT] = Ui.item(
            Clicks.material(interaction),
            "<ttl>${Clicks.short(interaction)} <hnt>· <val>${NpcFormat.joinState(joinState)}",
            listOf("<bd><val>${action.configuredTypes().size} <bd>of <val>$FIELD_COUNT <bd>fields set"),
            glowing = true,
        )

        ActionFieldsPane.fill(
            pane,
            ActionFieldsPane.Port(
                scope = ActionFieldsPane.Scope.NPC,
                player = player,
                action = action,
                textPrompts = context.textPrompts,
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
                openTitle = {
                    context.navigate(player, NpcEditorScreen.TitleEditor(config.id, interaction, joinState))
                },
            ),
        )

        pane.back(context, player)
        pane.left(
            ActionFieldsPane.COPY_SLOT,
            Ui.item(
                "PAPER",
                "<ttl>Copy This Action",
                listOf("<bd>Copies every field to the clipboard.", "", "<key>Left <hnt>Copy"),
            ),
        ) {
            if (existing != null && existing.configuredTypes().isNotEmpty()) {
                context.session(player).clipboard = existing.copy()
                context.render(player, screen)
            }
        }
        pane.left(
            ActionFieldsPane.WIPE_SLOT,
            Ui.item("RED_CONCRETE", "<err>Wipe This Action", listOf("", "<key>Left <hnt>Open confirmation")),
        ) {
            val purpose = ConfirmPurpose.WipeAction(interaction, joinState)
            context.navigate(player, NpcEditorScreen.Confirm(config.id, purpose))
        }
        pane.fillNavRow()

        return pane.menu(Ui.title(NpcFormat.cellName(interaction, joinState), NpcFormat.displayName(config)))
    }
}
