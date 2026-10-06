package app.simplecloud.npc.common.action

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClickCooldowns(private val now: () -> Long = System::currentTimeMillis) {

    private val until = ConcurrentHashMap<Pair<UUID, String>, Long>()

    fun tryAcquire(playerId: UUID, key: String, millis: Long): Boolean {
        if (millis <= 0) return true
        val time = now()
        until.values.removeIf { it <= time }

        return until.putIfAbsent(playerId to key, time + millis) == null
    }
}
