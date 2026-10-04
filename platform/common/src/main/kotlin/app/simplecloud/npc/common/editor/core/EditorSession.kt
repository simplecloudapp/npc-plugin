package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.menu.EditorMenu
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

open class EditorSession<S : Any> {

    val stack = ArrayDeque<S>()

    var menu: EditorMenu? = null
    var armedKey: String? = null
        private set
    var armedDeadlineMillis = 0L
        private set
    var armedSerial = 0
        private set
    var pickerResult: PickerResult? = null

    val pages = mutableMapOf<String, Int>()

    @Volatile
    private var promptOpenedAt = 0L

    fun markPromptOpen() {
        promptOpenedAt = System.currentTimeMillis()
    }

    fun promptClosed() {
        promptOpenedAt = 0L
    }

    fun consumeFreshPromptOpen(): Boolean {
        val openedAt = promptOpenedAt
        val fresh = openedAt != 0L && System.currentTimeMillis() - openedAt <= PROMPT_OPEN_WINDOW_MILLIS
        if (fresh) promptOpenedAt = 0L

        return fresh
    }

    fun arm(key: String, windowMillis: Long): Int {
        armedKey = key
        armedDeadlineMillis = System.currentTimeMillis() + windowMillis

        return ++armedSerial
    }

    fun disarm() {
        armedKey = null
        armedDeadlineMillis = 0L
    }

    fun isArmed(key: String): Boolean =
        armedKey == key && System.currentTimeMillis() < armedDeadlineMillis

    private companion object {
        private const val PROMPT_OPEN_WINDOW_MILLIS = 1_000L
    }
}

open class EditorSessions<T : EditorSession<*>>(private val create: () -> T) {

    private val sessions = ConcurrentHashMap<UUID, T>()

    fun of(playerId: UUID): T = sessions.getOrPut(playerId, create)

    fun peek(playerId: UUID): T? = sessions[playerId]

    fun forgetPlayer(playerId: UUID) {
        sessions.remove(playerId)
    }
}
