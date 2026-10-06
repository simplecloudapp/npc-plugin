package app.simplecloud.npc.bukkit.interact

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClickDedupeTracker<T>(private val windowMillis: Long = 150L) {

    private val lastInteract = ConcurrentHashMap<Pair<UUID, T>, Long>()

    internal val size: Int get() = lastInteract.size

    fun shouldHandle(playerId: UUID, targetId: T): Boolean {
        val now = System.currentTimeMillis()
        lastInteract.values.removeIf { now - it >= windowMillis }

        return lastInteract.putIfAbsent(playerId to targetId, now) == null
    }

    fun forgetPlayer(playerId: UUID) {
        lastInteract.keys.removeIf { it.first == playerId }
    }
}
