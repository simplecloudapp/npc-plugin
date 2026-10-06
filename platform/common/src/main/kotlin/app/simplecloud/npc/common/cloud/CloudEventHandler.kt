package app.simplecloud.npc.common.cloud

import app.simplecloud.api.CloudApi
import app.simplecloud.api.event.Subscription
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.render.HologramRotation
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.cloud.CloudListCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.logging.Level
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CloudEventHandler(
    private val cloudApi: CloudApi,
) {
    private var scope: CoroutineScope? = null
    private var debouncer: Debouncer? = null
    private val subscriptions = mutableListOf<Subscription>()

    private val refreshMutex = Mutex()
    private var periodicRefreshJob: Job? = null
    private var rotationJob: Job? = null
    private val rotation = HologramRotation()

    fun registerEvents(pluginContext: NpcPluginContext) {
        unregisterEvents()
        val scope = createScope().also { this.scope = it }
        debouncer = Debouncer(scope, EVENT_DEBOUNCE)

        val event = cloudApi.event()
        subscriptions += event.server().onUpdated { requestRefresh(pluginContext) }
        subscriptions += event.group().onUpdated { requestRefresh(pluginContext) }
        subscriptions += event.server().onStarted { requestRefresh(pluginContext) }
        subscriptions += event.server().onStopped { requestRefresh(pluginContext) }
        subscriptions += event.server().onStateChanged { requestRefresh(pluginContext) }
        subscriptions += event.server().onDeleted { requestRefresh(pluginContext) }

        periodicRefreshJob = scope.launch {
            while (isActive) {
                delay(PERIODIC_REFRESH)
                try {
                    CloudListCache.refreshNow()
                    refresh(pluginContext)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    logger.log(Level.WARNING, "Periodic hologram/inventory refresh failed", exception)
                }
            }
        }
        rotationJob = scope.launch {
            while (isActive) {
                delay(ROTATION_TICK)
                try {
                    rotate(pluginContext)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    logger.log(Level.WARNING, "Rotating hologram lines failed", exception)
                }
            }
        }
    }

    fun unregisterEvents() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = null
        rotationJob?.cancel()
        rotationJob = null
        subscriptions.forEach { runCatching { it.unsubscribe() } }
        subscriptions.clear()
        debouncer?.cancel()
        debouncer = null
        scope?.cancel()
        scope = null
    }

    private fun requestRefresh(pluginContext: NpcPluginContext) {
        debouncer?.debounce {
            CloudListCache.refreshNow()
            refresh(pluginContext)
        }
    }

    private suspend fun refresh(pluginContext: NpcPluginContext) = refreshMutex.withLock {
        pluginContext.npcRepository.findAll()
            .filter { it.hologram.enabled && !it.entity.needsRelocation }
            .forEach { config ->
                try {
                    pluginContext.hologramRenderer.createOrUpdate(config)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    logger.warning("Could not refresh hologram ${config.id}: ${exception.message}")
                }
            }

        pluginContext.inventoryViews.refreshOpenViews()
        pluginContext.editors.refreshOpenEditors(pluginContext.playerDirectory.onlinePlayers())
    }

    private suspend fun rotate(pluginContext: NpcPluginContext) {
        val visible = pluginContext.npcRepository.findAll()
            .filter { it.hologram.enabled && !it.entity.needsRelocation }
        val due = rotation.due(visible, System.currentTimeMillis())
        if (due.isEmpty()) return

        refreshMutex.withLock { due.forEach { pluginContext.hologramRenderer.createOrUpdate(it) } }
    }

    private fun createScope(): CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            logger.log(Level.WARNING, "Hologram/inventory refresh failed", throwable)
        },
    )

    companion object {
        private val EVENT_DEBOUNCE = 300.milliseconds

        private val PERIODIC_REFRESH = 3.seconds
        private val ROTATION_TICK = 250.milliseconds
        private val logger = NpcLog.logger
    }
}
