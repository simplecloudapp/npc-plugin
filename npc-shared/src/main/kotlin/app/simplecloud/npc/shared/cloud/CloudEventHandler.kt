package app.simplecloud.npc.shared.cloud

import app.simplecloud.api.CloudApi
import app.simplecloud.api.event.Subscription
import app.simplecloud.npc.shared.hologram.JoinStateHelper
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.utils.Debouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bukkit.Bukkit

class CloudEventHandler(
    private val cloudApi: CloudApi,
) {
    private var scope = createScope()
    private var debouncer = Debouncer(EVENT_DEBOUNCE_MILLIS)
    private val subscriptions = mutableListOf<Subscription>()
    private val refreshMutex = Mutex()
    private var periodicRefreshJob: Job? = null

    fun registerEvents(namespace: NpcNamespace) {
        unregisterEvents()
        scope = createScope()
        debouncer = Debouncer(EVENT_DEBOUNCE_MILLIS)

        val event = cloudApi.event()
        subscriptions += event.server().onUpdated { requestRefresh(namespace) }
        subscriptions += event.group().onUpdated { requestRefresh(namespace) }

        periodicRefreshJob = scope.launch {
            while (isActive) {
                delay(PERIODIC_REFRESH_MILLIS)
                updateHolograms(namespace)
            }
        }
    }

    fun unregisterEvents() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
        subscriptions.forEach { runCatching { it.unsubscribe() } }
        subscriptions.clear()
        debouncer.cancel()
        scope.cancel()
    }

    private fun requestRefresh(namespace: NpcNamespace) {
        debouncer.debounce(scope) { updateHolograms(namespace) }
    }

    private suspend fun updateHolograms(namespace: NpcNamespace) = refreshMutex.withLock {
        namespace.npcRepository.findAll().forEach { config ->
            try {
                val joinState = JoinStateHelper.getJoinState(config)
                namespace.hologramManager.updateTextHologram(config, joinState)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Bukkit.getLogger().warning(
                    "[SimpleCloud-NPC] Could not refresh hologram ${config.id}: ${exception.message}"
                )
            }
        }
    }

    private fun createScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val EVENT_DEBOUNCE_MILLIS = 1_000L
        private const val PERIODIC_REFRESH_MILLIS = 5_000L
    }
}
