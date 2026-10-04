package app.simplecloud.npc.common.utils

import app.simplecloud.npc.core.NpcLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.logging.Level

object BackgroundTasks {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            logger.log(Level.WARNING, "Unhandled background task failure", throwable)
        },
    )

    fun launch(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    fun shutdown() = scope.cancel()

    private val logger = NpcLog.logger
}
