package app.simplecloud.npc.core.cloud

import app.simplecloud.npc.core.NpcLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level

object CloudOutageLog {
    private const val THROTTLE_MILLIS = 60_000L

    private val logger = NpcLog.logger
    private val outage = AtomicBoolean(false)
    private val lastLogged = AtomicLong(0)
    private val throttled = ConcurrentHashMap<String, Long>()

    fun failed(cause: Throwable) {
        val now = System.currentTimeMillis()
        val first = outage.compareAndSet(false, true)
        val previous = lastLogged.get()

        if (!first && now - previous < THROTTLE_MILLIS) return
        if (!lastLogged.compareAndSet(previous, now)) return

        val summary = CloudUnavailableException(cause).message
        val message = if (first) {
            "$summary. NPC actions and holograms fall back to the last known cloud data until it is back."
        } else {
            "$summary (still unreachable)."
        }
        logger.warning(message)
        logger.log(Level.FINE, "Controller lookup failure", cause)
    }

    fun recovered() {
        if (outage.compareAndSet(true, false)) {
            lastLogged.set(0)
            throttled.clear()
            logger.info("SimpleCloud controller is reachable again.")
        }
    }

    fun warnThrottled(message: String, key: String = message) {
        val now = System.currentTimeMillis()
        var log = false
        throttled.compute(key) { _, previous ->
            if (previous != null && now - previous < THROTTLE_MILLIS) previous
            else now.also { log = true }
        }

        if (log) logger.warning(message)
    }
}
