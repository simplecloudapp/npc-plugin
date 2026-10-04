package app.simplecloud.npc.common.editor.npc.hologram

import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.core.pageKey
import app.simplecloud.npc.common.editor.core.pager
import app.simplecloud.npc.common.editor.core.toggle
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Paginator
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.ConfirmPurpose
import app.simplecloud.npc.common.editor.npc.HologramLiveState
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.editor.ui.Stepper
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.render.Providers
import java.util.Locale

object HologramMenuBuilder {
    private const val SIZE = 36

    private const val TOGGLE_SLOT = 3
    private const val HEIGHT_SLOT = 5
    private val GRID_SLOTS = 9..26
    private const val ADD_SLOT = 35

    private val START_HEIGHT = Stepper(
        label = "Start Height",
        range = NpcConfig.HologramConfigurationRoot.START_HEIGHT_RANGE,
        step = 0.05,
        default = NpcConfig.HologramConfigurationRoot().startHeight,
        decimals = 2,
        valueLine = { "<bd>Value <val>${String.format(Locale.ROOT, "%+.2f", it)} <hnt>blocks above the NPC" },
    )

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): EditorMenu {
        val hologram = config.hologram
        val layouts = NpcFormat.hologramJoinStates(config)
        val live = context.liveData.hologramState(config)
        val screen = NpcEditorScreen.Hologram(config.id)
        val pages = Paginator(context.session(player).pages, pageKey(screen), layouts, GRID_SLOTS.count())
        val pane = Pane(SIZE)

        fun edit(mutate: (NpcConfig.HologramConfigurationRoot) -> Unit) =
            context.commit(player, config.id, screen, Refresh.HOLOGRAM) { fresh -> fresh.also { mutate(it.hologram) } }

        pane.toggle(TOGGLE_SLOT, "Hologram", hologram.enabled, linkedHint(config)) { edit { it.enabled = !it.enabled } }
        pane[HEIGHT_SLOT] = START_HEIGHT.element(context.textPrompts, player, hologram.startHeight) { value ->
            edit { it.startHeight = value }
        }

        pages.visible.forEachIndexed { index, state ->
            pane.on(GRID_SLOTS.first + index, layoutItem(config, state, live)) { click ->
                when (click) {
                    MenuClick.LEFT -> context.navigate(player, NpcEditorScreen.HologramLines(config.id, state))

                    MenuClick.SHIFT_RIGHT -> if (!NpcFormat.isDefaultJoinState(state)) {
                        if (lineCount(config, state) == 0) {
                            edit { it.layouts.removeIf { layout -> layout.joinState.equals(state, true) } }
                        } else {
                            context.navigate(
                                player,
                                NpcEditorScreen.Confirm(config.id, ConfirmPurpose.RemoveLayout(state)),
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }

        pane.back(context, player)
        pane.pager(pages, listOf("<bd><val>${layouts.size} <bd>layouts", "<hnt>${pages.perPage} fit per page.")) {
            context.render(player, screen)
        }

        pane.left(ADD_SLOT, Ui.addHead("Add Layout")) {
            context.navigate(player, NpcEditorScreen.Picker(config.id, PickerPurpose.AddJoinState(forHologram = true)))
        }
        pane.fillNavRow()

        return pane.menu(Ui.title("Hologram", NpcFormat.displayName(config)))
    }

    private fun layoutItem(config: NpcConfig, state: String, live: HologramLiveState): NpcItem {
        val fallback = NpcFormat.isDefaultJoinState(state)
        val isLive = state == live.shown
        val lines = lineCount(config, state)
        val liveLine = when {
            !isLive -> null
            live.fallbackInUse -> "<on>Live now <hnt>· standing in for ${NpcFormat.joinState(live.joinState!!)}"
            else -> "<on>Live now"
        }
        val removeLine = if (fallback) "<hnt>The fallback cannot be removed." else "<key>Shift+Right <err>Remove"

        return Ui.item(
            "PAPER",
            if (isLive) "<on>${NpcFormat.joinState(state)}" else "<ttl>${NpcFormat.joinState(state)}",
            listOfNotNull(
                if (lines == 0) "<bd>Lines <warn>none yet" else "<bd>Lines <val>$lines",
                "",
                liveLine,
                "",
                "<key>Left <info>Edit lines",
                removeLine,
            ),
            glowing = isLive,
            amount = lines.coerceAtLeast(1),
        )
    }

    private fun linkedHint(config: NpcConfig): List<String> {
        if (!config.entity.providerLinked) return emptyList()
        val provider = Providers.displayName(config.entity.provider)

        return listOf(
            "",
            "<warn>Linked to $provider.",
            "<hnt>If the NPC has its own name or hologram",
            "<hnt>in $provider, turn it off there,",
            "<hnt>or both will overlap.",
        )
    }

    private fun lineCount(config: NpcConfig, state: String): Int = config.hologram.findLayout(state)?.lines?.size ?: 0
}
