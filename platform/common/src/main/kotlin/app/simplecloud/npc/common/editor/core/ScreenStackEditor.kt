package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.ChatInputPrompts
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.platform.EditorMenuOpener
import app.simplecloud.npc.core.platform.NpcPlayer

abstract class ScreenStackEditor<S : Any, T : EditorSession<S>>(
    protected val opener: EditorMenuOpener,
    protected val chatInputPrompts: ChatInputPrompts,
    val sessions: EditorSessions<T>,
) {

    protected abstract fun build(player: NpcPlayer, screen: S): EditorMenu?
    protected abstract fun exists(screen: S): Boolean

    protected open fun leavingScreen(session: T, popped: Boolean) {
        session.disarm()
        session.pickerResult = null
    }

    protected fun start(player: NpcPlayer, root: S) = opener.runSync {
        sessions.of(player.uniqueId).stack.clear()
        chatInputPrompts.cancel(player.uniqueId)
        navigate(player, root)
    }

    fun navigate(player: NpcPlayer, screen: S) {
        val session = sessions.peek(player.uniqueId) ?: return
        leavingScreen(session, popped = false)
        session.stack.addLast(screen)
        show(player, screen)
    }

    fun render(player: NpcPlayer, screen: S) {
        val session = sessions.peek(player.uniqueId) ?: return
        if (session.stack.lastOrNull() != screen) return
        show(player, screen)
    }

    fun replace(player: NpcPlayer, screen: S) {
        val session = sessions.peek(player.uniqueId) ?: return
        leavingScreen(session, popped = true)
        session.stack.removeLastOrNull()
        session.stack.addLast(screen)
        show(player, screen)
    }

    fun reset(player: NpcPlayer, screens: List<S>) {
        val session = sessions.peek(player.uniqueId) ?: return
        val top = screens.lastOrNull() ?: return close(player)
        leavingScreen(session, popped = true)
        session.stack.clear()
        session.stack.addAll(screens)
        show(player, top)
    }

    fun back(player: NpcPlayer, steps: Int = 1) {
        val session = sessions.peek(player.uniqueId) ?: return
        leavingScreen(session, popped = true)
        repeat(steps) { session.stack.removeLastOrNull() }
        val parent = session.stack.lastOrNull() ?: return close(player)
        show(player, parent)
    }

    open fun close(player: NpcPlayer) {
        sessions.forgetPlayer(player.uniqueId)
        chatInputPrompts.cancel(player.uniqueId)

        player.closeInventory()
    }

    fun refreshOpenScreens(players: List<NpcPlayer>) {
        val editing = players.filter { sessions.peek(it.uniqueId) != null }
        if (editing.isEmpty()) return

        opener.runSync {
            editing.forEach { player ->
                val session = sessions.peek(player.uniqueId) ?: return@forEach
                val screen = session.stack.lastOrNull() ?: return@forEach
                val menu = session.menu ?: return@forEach
                if (!opener.isShowing(player, menu)) return@forEach
                if (chatInputPrompts.isPending(player.uniqueId)) return@forEach

                render(player, screen)
            }
        }
    }

    fun handleEscape(player: NpcPlayer) {
        val session = sessions.peek(player.uniqueId) ?: return
        if (session.consumeFreshPromptOpen()) return
        if (chatInputPrompts.isPending(player.uniqueId)) return

        back(player)
    }

    protected open fun show(player: NpcPlayer, screen: S) {
        opener.runSync {
            val session = sessions.peek(player.uniqueId) ?: return@runSync
            val menu = build(player, screen) ?: return@runSync close(player)

            session.menu = menu
            opener.open(
                player,
                menu,
                onClick = { slot, click -> handleClick(player, screen, slot, click) },
                onClose = { handleEscape(player) },
            )
        }
    }

    private fun handleClick(player: NpcPlayer, screen: S, slot: Int, click: MenuClick) {
        if (click == MenuClick.DOUBLE_CLICK || click == MenuClick.OTHER) return

        if (!exists(screen)) return close(player)

        sessions.peek(player.uniqueId)?.menu?.click(slot, click)
    }
}
