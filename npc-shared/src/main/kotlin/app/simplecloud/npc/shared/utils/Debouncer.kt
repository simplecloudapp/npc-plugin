package app.simplecloud.npc.shared.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @author Niklas Nieberler
 */

class Debouncer(
    private val delayMillis: Long
) {

    private var job: Job? = null

    fun debounce(scope: CoroutineScope, action: suspend () -> Unit) {
        this.job?.cancel()
        this.job = scope.launch {
            delay(delayMillis)
            action()
        }
    }
}
