package app.simplecloud.npc.common.cloud

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration

class Debouncer(
    private val scope: CoroutineScope,
    private val delayDuration: Duration,
) {

    private val job = AtomicReference<Job?>(null)

    fun debounce(action: suspend () -> Unit) {
        val newJob = scope.launch {
            delay(delayDuration)
            action()
        }

        job.getAndSet(newJob)?.cancel()
    }

    fun cancel() {
        job.getAndSet(null)?.cancel()
    }
}
