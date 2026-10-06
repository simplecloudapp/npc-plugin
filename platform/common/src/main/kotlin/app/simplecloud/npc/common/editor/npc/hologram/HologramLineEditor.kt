package app.simplecloud.npc.common.editor.npc.hologram

import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptExtra
import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.common.editor.PromptTokens
import app.simplecloud.npc.common.editor.core.LineAction
import app.simplecloud.npc.common.editor.core.LineList
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.text.PlayerPlaceholders
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.flattener.ComponentFlattener

class LineTarget(val npcId: String, val joinState: String, val screen: NpcEditorScreen)

object HologramLineEditor {
    private const val MAX_VISIBLE_LENGTH = 64

    private val LINE_GONE = Component.text("That line no longer exists.")

    private val PLACEHOLDER = Regex("<[a-z]+_[a-z_]+>|%[^%\\s]+%")

    fun lines(
        context: NpcEditorContext,
        player: NpcPlayer,
        config: NpcConfig,
        joinState: String,
        screen: NpcEditorScreen,
    ): LineList<HologramLine> {
        val state = joinState.lowercase()
        val target = LineTarget(config.id, state, screen)

        return LineList(
            lines = config.hologram.findLayout(state)?.lines.orEmpty(),
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
            prompt = { index, current, onText, onDelete ->
                promptLine(context, player, target, index, current, onText, onDelete)
            },
            hints = { line ->
                val rotation = listOf(
                    "<hnt>Rotates <val>${line.texts.size} <hnt>texts every " +
                        "<val>${NpcFormat.seconds((line.interval * 20).toInt())}",
                ).takeIf { line.rotating }.orEmpty()
                val live = when {
                    line.texts.any(PlayerPlaceholders::isPersonal) -> listOf("<hnt>Shown per player")
                    line.texts.any(PLACEHOLDER::containsMatchIn) -> listOf("<hnt>Has placeholders")
                    else -> emptyList()
                }

                rotation + live
            },
            secondary = LineAction("Rotating texts") { index ->
                context.navigate(player, NpcEditorScreen.HologramFrames(config.id, state, index))
            },
            opensSecondary = HologramLine::rotating,
        )
    }

    private fun promptLine(
        context: NpcEditorContext,
        player: NpcPlayer,
        screen: LineTarget,
        index: Int?,
        current: String?,
        apply: (String) -> Unit,
        onDelete: (() -> Unit)?,
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
                onClear = onDelete,
                onCancel = { context.render(player, screen.screen) },
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
        screen: LineTarget,
        index: Int,
    ) {
        val line = context.npcRepository.find(screen.npcId)
            ?.hologram?.findLayout(screen.joinState)?.lines?.getOrNull(index)
            ?: return context.render(player, screen.screen)

        context.chatPrompt(
            player,
            Prompt(
                title = "Rotating Text",
                instruction = "Type text {} of line {}; it takes turns with the others.",
                instructionArgs = listOf(line.texts.size + 1, index + 1),
                format = PromptFormat.MINI_MESSAGE,
                placeholders = PromptTokens.HOLOGRAM,
                onCancel = { context.render(player, screen.screen) },
                onSubmit = { input ->
                    val text = input.trim()
                    tooLong(text)?.let { return@Prompt it }
                    context.commit(player, screen.npcId, screen.screen, Refresh.HOLOGRAM) { fresh ->
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
        screen: LineTarget,
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
        screen: LineTarget,
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
