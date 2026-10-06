package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.ChatInputPrompts
import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.core.platform.NpcPlayer

typealias ScreenAction<S> = (NpcPlayer, S) -> Unit

interface EditorHost<S : Any> {
    val sessions: EditorSessions<out EditorSession<S>>
    val chatInputPrompts: ChatInputPrompts
    val textPrompts: TextPrompts
    val runLater: (delayTicks: Long, () -> Unit) -> Unit

    fun render(player: NpcPlayer, screen: S)
    fun navigate(player: NpcPlayer, screen: S)
    fun back(player: NpcPlayer, steps: Int = 1)
    fun close(player: NpcPlayer)

    fun session(player: NpcPlayer): EditorSession<S> = sessions.of(player.uniqueId)

    fun isShowing(player: NpcPlayer, screen: (S) -> Boolean): Boolean =
        sessions.peek(player.uniqueId)?.stack?.lastOrNull()?.let(screen) == true

    fun promptContext(screen: S): String? = null

    fun chatPrompt(player: NpcPlayer, prompt: Prompt) {
        val top = sessions.peek(player.uniqueId)?.stack?.lastOrNull()
        startChatPrompt(sessions, chatInputPrompts, player, prompt.copy(context = top?.let(::promptContext)))
    }
}
