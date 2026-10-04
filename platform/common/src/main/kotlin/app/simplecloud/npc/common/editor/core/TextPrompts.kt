package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.ChatInputPrompts
import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptExtra
import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.PromptPlaceholder
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.core.platform.NpcPlayer
import net.kyori.adventure.text.Component

class TextPrompts(private val start: (NpcPlayer, Prompt) -> Unit) {

    fun open(
        player: NpcPlayer,
        title: String,
        instruction: String,
        current: String?,
        format: PromptFormat = PromptFormat.PLAIN,
        placeholders: List<PromptPlaceholder> = emptyList(),
        onClear: (() -> Unit)? = null,
        validate: (String) -> String? = { null },
        onResult: (String) -> Unit,
    ) = start(
        player,
        Prompt(
            title = title,
            instruction = instruction,
            current = current?.ifBlank { null },
            format = format,
            placeholders = placeholders,
            onClear = onClear,
            onSubmit = { text ->
                validate(text)?.let { return@Prompt PromptResult.Rejected(Component.text(it)) }
                onResult(text)
                PromptResult.Accepted
            },
        ),
    )

    companion object {
        fun <S : Any> chat(
            sessions: EditorSessions<out EditorSession<S>>,
            chatInputPrompts: ChatInputPrompts,
            render: ScreenAction<S>,
            context: (S) -> String?,
        ) = TextPrompts { player, prompt ->
            val top = sessions.peek(player.uniqueId)?.stack?.lastOrNull()
            startChatPrompt(
                sessions,
                chatInputPrompts,
                player,
                prompt.copy(
                    context = top?.let(context),
                    onCancel = { sessions.peek(player.uniqueId)?.stack?.lastOrNull()?.let { render(player, it) } },
                ),
            )
        }
    }
}

fun startChatPrompt(
    sessions: EditorSessions<out EditorSession<*>>,
    chatInputPrompts: ChatInputPrompts,
    player: NpcPlayer,
    prompt: Prompt,
) {
    chatInputPrompts.cancel(player.uniqueId)
    sessions.peek(player.uniqueId)?.markPromptOpen()
    player.closeInventory()
    chatInputPrompts.start(
        player,
        prompt.copy(
            onCancel = {
                sessions.peek(player.uniqueId)?.promptClosed()
                prompt.onCancel()
            },
            onClear = prompt.onClear?.let { clear ->
                {
                    sessions.peek(player.uniqueId)?.promptClosed()
                    clear()
                }
            },
            onSubmit = { text -> prompt.onSubmit(text).also { closeOnAccept(sessions, player, it) } },
            extra = prompt.extra?.let { extra ->
                PromptExtra(
                    extra.label,
                    extra.hint,
                    submit = { text -> extra.submit(text).also { closeOnAccept(sessions, player, it) } },
                    next = extra.next,
                )
            },
        ),
    )
}

private fun closeOnAccept(sessions: EditorSessions<out EditorSession<*>>, player: NpcPlayer, result: PromptResult) {
    if (result is PromptResult.Accepted) sessions.peek(player.uniqueId)?.promptClosed()
}
