package app.simplecloud.npc.common.editor.npc.action

import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.ConfirmPurpose
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.ui.Clicks
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.platform.NpcPlayer

object ActionMatrixMenuBuilder {
    private const val SIZE = 54

    private val HEADER_SLOTS = 1..8
    private const val CLIPBOARD_SLOT = 47

    private fun rowLabelSlot(row: Int): Int = (row + 1) * 9
    private fun cellSlot(row: Int, column: Int): Int = (row + 1) * 9 + 1 + column

    private val EMPTY_CELL = Ui.item(
        Pane.GRAY_PANE,
        "<off>Not configured",
        listOf(
            "",
            "<key>Left <hnt>Configure",
            "<key>Shift+Right <hnt>Paste copied action",
        ),
    )

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): EditorMenu {
        val joinStates = NpcFormat.actionJoinStates(config).take(NpcConfig.MAX_JOIN_STATES)
        val session = context.session(player)
        val pane = Pane(SIZE)

        joinStates.forEachIndexed { column, state ->
            val configured =
                NpcFormat.CLICK_ROWS.count { config.findAction(it, state)?.configuredTypes()?.isNotEmpty() == true }
            val fallback = NpcFormat.isDefaultJoinState(state)
            val removeLine = if (fallback) {
                "<hnt>The fallback join state cannot be removed."
            } else {
                "<key>Shift+Right <err>Remove join state"
            }
            val header = Ui.item(
                "PAPER",
                "<ttl>${NpcFormat.joinState(state)}",
                listOf(
                    "<bd>Configured <val>$configured <bd>of <val>${NpcFormat.CLICK_ROWS.size} <bd>clicks",
                    "",
                    removeLine,
                ),
            )
            if (fallback) {
                pane[HEADER_SLOTS.first + column] = header
            } else {
                pane.on(HEADER_SLOTS.first + column, header, MenuClick.SHIFT_RIGHT) {
                    context.navigate(player, NpcEditorScreen.Confirm(config.id, ConfirmPurpose.RemoveJoinState(state)))
                }
            }
        }

        if (joinStates.size < NpcConfig.MAX_JOIN_STATES) {
            pane.left(
                HEADER_SLOTS.first + joinStates.size,
                Ui.addHead("Add Join State", listOf("", "<key>Left <info>Pick a join state")),
            ) {
                context.navigate(
                    player,
                    NpcEditorScreen.Picker(config.id, PickerPurpose.AddJoinState(forHologram = false)),
                )
            }
        }

        NpcFormat.CLICK_ROWS.forEachIndexed { row, interaction ->
            pane[rowLabelSlot(row)] =
                Ui.item(Clicks.material(interaction), "<ttl>${Clicks.label(interaction)}")

            joinStates.forEachIndexed { column, state ->
                val existing = config.findAction(interaction, state)?.takeIf { it.configuredTypes().isNotEmpty() }
                val icon = existing?.let { cellItem(interaction, state, it) } ?: EMPTY_CELL
                pane.on(cellSlot(row, column), icon) { click ->
                    handleCellClick(context, player, config, interaction, state, existing, click)
                }
            }
        }

        pane.back(context, player)

        val copied = session.clipboard
        if (copied != null) {
            pane.on(
                CLIPBOARD_SLOT,
                Ui.item(
                    "PAPER",
                    "<ttl>Clipboard",
                    listOf(
                        "<bd>Holding <val>${NpcFormat.cellName(copied.interactionType, copied.joinState)}",
                        "<bd><val>${copied.configuredTypes().size} <bd>fields",
                        "",
                        "<hnt>Shift+Right any cell to paste.",
                        "<hnt>Pasting overwrites without asking!",
                        "",
                        "<key>Q <hnt>Clear clipboard",
                    ),
                ),
                MenuClick.DROP,
            ) {
                session.clipboard = null
                context.render(player, NpcEditorScreen.ActionMatrix(config.id))
            }
        } else {
            pane[CLIPBOARD_SLOT] = Ui.item(
                "PAPER",
                "<off>Clipboard",
                listOf("<hnt>Nothing copied yet.", "", "<hnt>Shift+Left a configured cell to copy."),
            )
        }
        pane.fillNavRow()

        return pane.menu(Ui.title("Actions", NpcFormat.displayName(config)))
    }

    private fun cellItem(
        interaction: PlayerInteraction,
        state: String,
        action: NpcConfig.ActionConfiguration,
    ): NpcItem =
        Ui.item(
            "LIME_CONCRETE",
            "<on>${Clicks.short(interaction)} <hnt>· <val>${NpcFormat.joinState(state)}",
            listOfNotNull("", "<on>Counts as join target".takeIf { action.joinTarget }) +
                NpcFormat.fieldLines(action) +
                listOf(
                    "",
                    "<key>Left <hnt>Edit  <key>Shift+Left <hnt>Copy",
                    "<key>Shift+Right <hnt>Paste  <key>Q <err>Wipe",
                ),
        )

    private fun handleCellClick(
        context: NpcEditorContext,
        player: NpcPlayer,
        config: NpcConfig,
        interaction: PlayerInteraction,
        state: String,
        existing: NpcConfig.ActionConfiguration?,
        click: MenuClick,
    ) {
        val session = context.session(player)
        val matrix = NpcEditorScreen.ActionMatrix(config.id)

        when (click) {
            MenuClick.LEFT -> context.navigate(player, NpcEditorScreen.ActionEditor(config.id, interaction, state))

            MenuClick.SHIFT_LEFT -> if (existing != null) {
                session.clipboard = existing.copy()
                context.render(player, matrix)
            }

            MenuClick.SHIFT_RIGHT -> session.clipboard?.let { copied ->
                context.commit(player, config.id, matrix) { fresh ->
                    fresh.actions.removeIf { it.interactionType == interaction && it.joinState.equals(state, true) }
                    if (copied.joinTarget) fresh.actions.forEach { it.joinTarget = false }
                    fresh.actions += copied.copy(interactionType = interaction, joinState = state)
                    fresh
                }
            }

            MenuClick.DROP -> if (existing != null) {
                context.navigate(
                    player,
                    NpcEditorScreen.Confirm(config.id, ConfirmPurpose.WipeAction(interaction, state)),
                )
            }

            else -> Unit
        }
    }
}
