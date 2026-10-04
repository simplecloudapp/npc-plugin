package app.simplecloud.npc.bukkit.hologram

import app.simplecloud.npc.core.NpcLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.logging.Level

class RefreshScope(logLabel: String) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            NpcLog.logger.log(Level.WARNING, "Unhandled $logLabel refresh failure", throwable)
        },
    )

    private class Lock {
        val mutex = Mutex()
        var users = 0
    }

    private val locks = HashMap<String, Lock>()

    fun launch(id: String, block: suspend () -> Unit) {
        scope.launch {
            val lock = synchronized(locks) { locks.getOrPut(id, ::Lock).also { it.users++ } }
            try {
                lock.mutex.withLock { block() }
            } finally {
                synchronized(locks) { if (--lock.users == 0) locks.remove(id) }
            }
        }
    }

    fun cancel() = scope.cancel()
}
