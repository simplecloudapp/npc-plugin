package app.simplecloud.npc.common.editor.npc.hologram

import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptExtra
import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.common.editor.PromptTokens
import app.simplecloud.npc.common.editor.core.LineAction
import app.simplecloud.npc.common.editor.core.LineList
import app.simplecloud.npc.common.editor.core.LinesPane
import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.text.PlayerPlaceholders
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.flattener.ComponentFlattener

object HologramLinesMenuBuilder {
    private const val CONTROLS_SLOT = 30
    private const val LAYOUT_SLOT = 32

    private const val MAX_VISIBLE_LENGTH = 64

    private val LINE_GONE = Component.text("That line no longer exists.")

    private val PLACEHOLDER = Regex("<[a-z]+_[a-z_]+>|%[^%\\s]+%")

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, joinState: String): EditorMenu {
        val state = joinState.lowercase()
        val layoutLines = config.hologram.findLayout(state)?.lines.orEmpty()
        val screen = NpcEditorScreen.HologramLines(config.id, state)
        val pane = Pane(LinesPane.SIZE)

        val lines = LineList(
            lines = layoutLines,
            textOf = HologramLine::shownText,
            withText = { line, text ->
                if (line.rotating) line.withFrames(listOf(text) + line.texts.drop(1)) else line.copy(text = text)
            },
            create = ::HologramLine,
            edit = { mutate ->
                context.commit(player, config.id, screen, Refresh.HOLOGRAM) { fresh ->
                    fresh.takeIf { mutate(it.hologram.layoutOrCreate(state).lines) }
                }
            },
            prompt = { index, current, onText -> promptLine(context, player, screen, index, current, onText) },
            hints = { line ->
                val rotation = listOf(
                    "<hnt>Rotates <val>${line.texts.size} <hnt>texts every " +
                        "<val>${NpcFormat.seconds((line.interval * 20).toInt())}<hnt>.",
                ).takeIf { line.rotating }.orEmpty()
                val placeholders = listOf("<hnt>Contains a placeholder; refreshes", "<hnt>every few seconds.")
                    .takeIf { line.texts.any(PLACEHOLDER::containsMatchIn) }
                    .orEmpty()
                val personal = when {
                    line.texts.none(PlayerPlaceholders::isPersonal) -> emptyList()
                    line.texts.any(PlayerPlaceholders::usesExternal) -> listOf(
                        "<hnt>Shown per player; PlaceholderAPI",
                        "<hnt>values update every <val>${NpcFormat.seconds((line.refreshSeconds * 20).toInt(), 2)}<hnt>.",
                    )
                    else -> listOf("<hnt>Shown per player.")
                }

                rotation + placeholders + personal
            },
            secondary = LineAction("Rotating texts") { index ->
                context.navigate(player, NpcEditorScreen.HologramFrames(config.id, state, index))
            },
        )
        LinesPane.place(pane, lines, CONTROLS_SLOT)
        pane[LAYOUT_SLOT] = Ui.item(
            "GLOW_ITEM_FRAME",
            "<ttl>Layout <val>${NpcFormat.joinState(state)}",
            listOf(
                "<bd><val>${layoutLines.size} <bd>lines",
                "<bd>Visible while the target's join state",
                "<bd>is <val>${NpcFormat.joinState(state)}<bd>.",
            ),
        )
        pane.back(context, player)
        pane.fillNavRow()

        return pane.menu(Ui.title("Lines · ${NpcFormat.joinState(state)}", NpcFormat.displayName(config)))
    }

    private fun promptLine(
        context: NpcEditorContext,
        player: NpcPlayer,
        screen: NpcEditorScreen.HologramLines,
        index: Int?,
        current: String?,
        apply: (String) -> Unit,
    ) {
        var target = index

        context.chatPrompt(
            player,
            Prompt(
                title = "Hologram Line",
                instruction = "Type the hologram line in chat.",
                format = PromptFormat.MINI_MESSAGE,
                placeholders = PromptTokens.HOLOGRAM,
                current = current,
                onCancel = { context.render(player, screen) },
                onSubmit = { input ->
                    val text = input.trim()
                    tooLong(text)?.let { return@Prompt it }
                    apply(text)
                    PromptResult.Accepted
                },
                extra = rotateExtra(
                    save = { text -> saveLine(context, player, screen, target, text)?.also { target = it } != null },
                    next = { target?.let { promptNextText(context, player, screen, it) } },
                ),
            ),
        )
    }

    private fun promptNextText(
        context: NpcEditorContext,
        player: NpcPlayer,
        screen: NpcEditorScreen.HologramLines,
        index: Int,
    ) {
        val line = context.npcRepository.find(screen.npcId)
            ?.hologram?.findLayout(screen.joinState)?.lines?.getOrNull(index) ?: return context.render(player, screen)

        context.chatPrompt(
            player,
            Prompt(
                title = "Rotating Text",
                instruction = "Type text {} of line {}; it takes turns with the others.",
                instructionArgs = listOf(line.texts.size + 1, index + 1),
                format = PromptFormat.MINI_MESSAGE,
                placeholders = PromptTokens.HOLOGRAM,
                onCancel = { context.render(player, screen) },
                onSubmit = { input ->
                    val text = input.trim()
                    tooLong(text)?.let { return@Prompt it }
                    context.commit(player, screen.npcId, screen, Refresh.HOLOGRAM) { fresh ->
                        appendText(fresh, screen, index, text)
                    }
                    PromptResult.Accepted
                },
                extra = rotateExtra(
                    save = { text ->
                        context.change(player, screen.npcId, Refresh.HOLOGRAM) { appendText(it, screen, index, text) }
                    },
                    next = { promptNextText(context, player, screen, index) },
                ),
            ),
        )
    }

    private fun rotateExtra(save: (String) -> Boolean, next: () -> Unit) = PromptExtra(
        label = "Apply + add rotating text",
        hint = "Saves it and asks right away for another text this line takes turns with.",
        submit = { input ->
            val text = input.trim()
            tooLong(text) ?: if (save(text)) PromptResult.Accepted else PromptResult.Rejected(LINE_GONE)
        },
        next = next,
    )

    private fun saveLine(
        context: NpcEditorContext,
        player: NpcPlayer,
        screen: NpcEditorScreen.HologramLines,
        index: Int?,
        text: String,
    ): Int? {
        var saved: Int? = null
        context.change(player, screen.npcId, Refresh.HOLOGRAM) { fresh ->
            val lines = fresh.hologram.layoutOrCreate(screen.joinState).lines
            when (index) {
                null -> {
                    lines += HologramLine(text)
                    saved = lines.lastIndex
                }

                in lines.indices -> {
                    val line = lines[index]
                    lines[index] = line.withFrames(listOf(text) + line.texts.drop(1))
                    saved = index
                }

                else -> return@change null
            }
            fresh
        }

        return saved
    }

    private fun appendText(
        fresh: NpcConfig,
        screen: NpcEditorScreen.HologramLines,
        index: Int,
        text: String,
    ): NpcConfig? {
        val lines = fresh.hologram.findLayout(screen.joinState)?.lines ?: return null
        val line = lines.getOrNull(index) ?: return null
        lines[index] = line.withFrames(line.texts + text)

        return fresh
    }

    fun tooLong(text: String): PromptResult.Rejected? {
        val visible = visibleLength(text)
        if (visible <= MAX_VISIBLE_LENGTH) return null

        return PromptResult.Rejected(
            Component.text("That line shows $visible characters, the maximum is $MAX_VISIBLE_LENGTH."),
        )
    }

    private fun visibleLength(text: String): Int {
        val component: Component = runCatching { Msg.miniMessage.deserialize(text) }.getOrElse { Component.text(text) }
        return buildString { ComponentFlattener.basic().flatten(component) { append(it) } }.length
    }
}
