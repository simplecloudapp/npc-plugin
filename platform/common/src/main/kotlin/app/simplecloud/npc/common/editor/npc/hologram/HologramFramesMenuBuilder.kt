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
    private const val INTERVAL_SLOT = 22
    private const val REFRESH_SLOT = 24
    private val FRAME_SLOTS = (0..17).toList()

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
        hints = listOf("<hnt>How often PlaceholderAPI values update."),
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
            prompt = { _, current, onText, onDelete ->
                promptFrame(context, player, screen, current, onText, onDelete)
            },
        )
        LinesPane.place(pane, frames, FRAME_SLOTS, "Add Text")

        val interval = line?.interval ?: HologramLine.DEFAULT_FRAME_INTERVAL
        pane[INTERVAL_SLOT] = INTERVAL.element(interval) { value ->
            editLine { it.copy(frameInterval = value) }
        }
        if (line != null && line.texts.any(PlayerPlaceholders::usesExternal)) {
            pane[REFRESH_SLOT] = REFRESH.element(line.refreshSeconds) { value ->
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
        onDelete: (() -> Unit)?,
    ) {
        context.chatPrompt(
            player,
            Prompt(
                title = "Rotating Text",
                instruction = "Type one of the texts this line rotates through.",
                current = current,
                format = PromptFormat.MINI_MESSAGE,
                placeholders = PromptTokens.HOLOGRAM,
                onClear = onDelete,
                onCancel = { context.render(player, screen) },
                onSubmit = { input ->
                    val text = input.trim()
                    HologramLineEditor.tooLong(text)?.let { return@Prompt it }
                    apply(text)
                    PromptResult.Accepted
                },
            ),
        )
    }
}
