package app.simplecloud.npc.common.editor.npc.tabs

import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.npc.ConfirmPurpose
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.NpcTab
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.ui.Clicks
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.platform.NpcPlayer

object ActionsTabBuilder {
    private const val ADD_STATE_SLOT = 26
    private const val SUMMARY_LINES = 3
    private val CARDS = listOf(
        PlayerInteraction.LEFT_CLICK to 28,
        PlayerInteraction.RIGHT_CLICK to 30,
        PlayerInteraction.SHIFT_LEFT_CLICK to 32,
        PlayerInteraction.SHIFT_RIGHT_CLICK to 34,
    )

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): EditorMenu {
        val screen = NpcEditorScreen.Tab(config.id, NpcTab.ACTIONS)
        val session = context.session(player)
        val states = NpcFormat.actionJoinStates(config)
        val live = context.liveData.hologramState(config).joinState?.lowercase()
        val selected = session.actionState?.takeIf { it in states }
            ?: live?.takeIf { it in states }
            ?: NpcConfig.DEFAULT_JOIN_STATE.lowercase()
        val pane = TabFrame.pane(context, player, config, NpcTab.ACTIONS)

        StateChipRow.place(
            pane,
            context,
            player,
            screen,
            "action-states:${config.id}",
            StateChips(
                states = states,
                selected = selected,
                live = live,
                detail = { state -> "<val>${configured(config, state)} <hnt>of ${CARDS.size} clicks set" },
                select = { state ->
                    session.actionState = state
                    context.render(player, screen)
                },
                remove = { state ->
                    context.navigate(player, NpcEditorScreen.Confirm(config.id, ConfirmPurpose.RemoveJoinState(state)))
                },
            ),
        )

        CARDS.forEach { (interaction, slot) ->
            val action = config.findAction(interaction, selected)?.takeIf { it.configuredTypes().isNotEmpty() }
            val item = if (action == null) {
                Ui.item(
                    "LIGHT_GRAY_CONCRETE",
                    "<ttl>${Clicks.label(interaction)}",
                    listOf("<off>Nothing happens", "<key>Left <hnt>Set up"),
                )
            } else {
                val summary =
                    listOfNotNull("<on>Join target".takeIf { action.joinTarget }) + NpcFormat.fieldLines(action)
                Ui.item(
                    "LIME_CONCRETE",
                    "<on>${Clicks.label(interaction)}",
                    summary.take(SUMMARY_LINES) + listOf("<key>Left <hnt>Edit"),
                )
            }
            pane.left(slot, item) {
                context.navigate(player, NpcEditorScreen.ActionEditor(config.id, interaction, selected))
            }
        }

        if (states.size < NpcConfig.MAX_JOIN_STATES) {
            val add = Ui.addHead("Add Join State", listOf("<hnt>Other clicks for another join state"))
            pane.left(ADD_STATE_SLOT, add) {
                val purpose = PickerPurpose.AddJoinState(forHologram = false)
                context.navigate(player, NpcEditorScreen.Picker(config.id, purpose))
            }
        }

        return TabFrame.menu(pane, config)
    }

    private fun configured(config: NpcConfig, state: String): Int =
        CARDS.count { (interaction, _) ->
            config.findAction(interaction, state)?.configuredTypes()?.isNotEmpty() == true
        }
}
