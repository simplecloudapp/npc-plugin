package app.simplecloud.npc.common.editor.npc.tabs

import app.simplecloud.npc.common.editor.core.LinesPane
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.npc.ConfirmPurpose
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.NpcTab
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.editor.npc.hologram.HologramLineEditor
import app.simplecloud.npc.common.editor.ui.Stepper
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.render.Providers
import java.util.Locale

object HologramTabBuilder {
    private const val TOGGLE_SLOT = 18
    private const val HEIGHT_SLOT = 19
    private const val ADD_LAYOUT_SLOT = 26
    private val LINE_SLOTS = (27..44).toList()

    private val START_HEIGHT = Stepper(
        label = "Height",
        range = NpcConfig.HologramConfigurationRoot.START_HEIGHT_RANGE,
        step = 0.05,
        default = NpcConfig.HologramConfigurationRoot().startHeight,
        decimals = 2,
        valueLine = { "<val>${String.format(Locale.ROOT, "%+.2f", it)} <hnt>blocks" },
    )

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): EditorMenu {
        val hologram = config.hologram
        val screen = NpcEditorScreen.Tab(config.id, NpcTab.HOLOGRAM)
        val session = context.session(player)
        val layouts = NpcFormat.hologramJoinStates(config)
        val live = context.liveData.hologramState(config).shown
        val selected = session.hologramState?.takeIf { it in layouts }
            ?: live?.takeIf { it in layouts }
            ?: NpcConfig.DEFAULT_JOIN_STATE.lowercase()
        val pane = TabFrame.pane(context, player, config, NpcTab.HOLOGRAM)

        fun edit(mutate: (NpcConfig.HologramConfigurationRoot) -> Unit) =
            context.commit(player, config.id, screen, Refresh.HOLOGRAM) { fresh -> fresh.also { mutate(it.hologram) } }

        pane.left(TOGGLE_SLOT, Ui.toggle("Hologram", hologram.enabled, linkedHint(config), "GLOW_ITEM_FRAME")) {
            edit { it.enabled = !it.enabled }
        }
        pane[HEIGHT_SLOT] = START_HEIGHT.element(hologram.startHeight) { value -> edit { it.startHeight = value } }

        StateChipRow.place(
            pane,
            context,
            player,
            screen,
            "hologram-layouts:${config.id}",
            StateChips(
                states = layouts,
                selected = selected,
                live = live,
                detail = { state -> "<val>${lineCount(config, state)} <hnt>lines" },
                select = { state ->
                    session.hologramState = state
                    context.render(player, screen)
                },
                remove = { state ->
                    if (lineCount(config, state) == 0) {
                        edit { it.layouts.removeIf { layout -> layout.joinState.equals(state, true) } }
                    } else {
                        context.navigate(player, NpcEditorScreen.Confirm(config.id, ConfirmPurpose.RemoveLayout(state)))
                    }
                },
            ),
        )

        LinesPane.place(pane, HologramLineEditor.lines(context, player, config, selected, screen), LINE_SLOTS)

        pane.left(ADD_LAYOUT_SLOT, Ui.addHead("Add Layout", listOf("<hnt>Other lines for another join state"))) {
            context.navigate(player, NpcEditorScreen.Picker(config.id, PickerPurpose.AddJoinState(forHologram = true)))
        }

        return TabFrame.menu(pane, config)
    }

    private fun linkedHint(config: NpcConfig): List<String> {
        if (!config.entity.providerLinked) return emptyList()

        return listOf("<warn>Turn off ${Providers.displayName(config.entity.provider)}'s own name or both show.")
    }

    private fun lineCount(config: NpcConfig, state: String): Int = config.hologram.findLayout(state)?.lines?.size ?: 0
}
