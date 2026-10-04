package app.simplecloud.npc.common.editor.npc.hologram

import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.common.editor.PromptTokens
import app.simplecloud.npc.common.editor.core.LineList
import app.simplecloud.npc.common.editor.core.LinesPane
import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.editor.ui.Stepper
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.text.PlayerPlaceholders

object HologramFramesMenuBuilder {
    private const val SIZE = 27
    private const val CONTROLS_SLOT = 21
    private const val INTERVAL_SLOT = 23
    private const val REFRESH_SLOT = 25

    private val INTERVAL = Stepper(
        label = "Interval",
        range = HologramLine.FRAME_INTERVAL_RANGE,
        step = 0.5,
        default = HologramLine.DEFAULT_FRAME_INTERVAL,
        decimals = 1,
        unit = "seconds",
        hints = listOf("<hnt>How long each text stays."),
    )

    private val REFRESH = Stepper(
        label = "Placeholder Refresh",
        range = HologramLine.REFRESH_RANGE,
        step = 0.25,
        default = HologramLine.DEFAULT_REFRESH,
        decimals = 2,
        unit = "seconds",
        hints = listOf(
            "<hnt>How often PlaceholderAPI values",
            "<hnt>are looked up for each viewer.",
            "<hnt>Raise it for slow placeholders.",
        ),
    )

    fun build(
        context: NpcEditorContext,
        player: NpcPlayer,
        config: NpcConfig,
        screen: NpcEditorScreen.HologramFrames,
    ): EditorMenu {
        val line = config.hologram.findLayout(screen.joinState)?.lines?.getOrNull(screen.line)
        val pane = Pane(SIZE)

        fun editLine(mutate: (HologramLine) -> HologramLine?) =
            context.commit(player, config.id, screen, Refresh.HOLOGRAM) { fresh ->
                val lines = fresh.hologram.findLayout(screen.joinState)?.lines ?: return@commit null
                val current = lines.getOrNull(screen.line) ?: return@commit null
                lines[screen.line] = mutate(current) ?: return@commit null
                fresh
            }

        val frames = LineList(
            lines = line?.texts.orEmpty(),
            textOf = { it },
            withText = { _, text -> text },
            create = { it },
            edit = { mutate ->
                editLine { current ->
                    val updated = current.texts.toMutableList()
                    if (mutate(updated)) current.withFrames(updated) else null
                }
            },
            prompt = { _, current, onText -> promptFrame(context, player, screen, current, onText) },
        )
        LinesPane.place(pane, frames, CONTROLS_SLOT, "<hnt>Add a second text to start rotating.", "Add Text")

        val interval = line?.interval ?: HologramLine.DEFAULT_FRAME_INTERVAL
        pane[INTERVAL_SLOT] = INTERVAL.element(context.textPrompts, player, interval) { value ->
            editLine { it.copy(frameInterval = value) }
        }
        if (line != null && line.texts.any(PlayerPlaceholders::usesExternal)) {
            pane[REFRESH_SLOT] = REFRESH.element(context.textPrompts, player, line.refreshSeconds) { value ->
                editLine { it.copy(refresh = value) }
            }
        }
        pane.back(context, player)
        pane.fillNavRow()

        val title = "Rotating · ${NpcFormat.joinState(screen.joinState)} #${screen.line + 1}"

        return pane.menu(Ui.title(title, NpcFormat.displayName(config)))
    }

    private fun promptFrame(
        context: NpcEditorContext,
        player: NpcPlayer,
        screen: NpcEditorScreen.HologramFrames,
        current: String?,
        apply: (String) -> Unit,
    ) {
        context.chatPrompt(
            player,
            Prompt(
                title = "Rotating Text",
                instruction = "Type one of the texts this line rotates through.",
                current = current,
                format = PromptFormat.MINI_MESSAGE,
                placeholders = PromptTokens.HOLOGRAM,
                onCancel = { context.render(player, screen) },
                onSubmit = { input ->
                    val text = input.trim()
                    HologramLinesMenuBuilder.tooLong(text)?.let { return@Prompt it }
                    apply(text)
                    PromptResult.Accepted
                },
            ),
        )
    }
}
