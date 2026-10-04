package app.simplecloud.npc.core.cloud

import app.simplecloud.api.group.Group
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.server.Server
import app.simplecloud.npc.core.NpcLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.logging.Level

class CloudListStore(
    private val fetchGroups: suspend () -> List<Group>,
    private val fetchPersistentServers: suspend () -> List<PersistentServer>,
    private val fetchServers: suspend () -> List<Server>,
    private val fetchAvailableServers: suspend (groupName: String) -> List<Server>,
    private val now: () -> Long = System::currentTimeMillis,
    private val ttlMillis: Long = TTL_MILLIS,
    launchInBackground: ((suspend () -> Unit) -> Unit)? = null,
) : CloudLists, CloudSnapshots {

    private class Cached<T>(val value: T, val fetchedAt: Long)

    private class Lists(val groups: List<Group>, val persistentServers: List<PersistentServer>)

    private val ownScope: CoroutineScope? = if (launchInBackground != null) null else CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            NpcLog.logger.log(Level.FINE, "Cloud cache revalidation failed", throwable)
        },
    )

    private val launchTask: (suspend () -> Unit) -> Unit =
        launchInBackground ?: { block -> ownScope?.launch(start = CoroutineStart.ATOMIC) { block() } }

    @Volatile
    private var lists: Cached<Lists>? = null

    @Volatile
    private var servers: Cached<List<Server>>? = null

    @Volatile
    private var snapshot: CloudSnapshot = CloudSnapshot.EMPTY

    @Volatile
    private var lastFailure: Pair<Long, Throwable>? = null

    private val listsMutex = Mutex()
    private val serversMutex = Mutex()

    override fun peek(): CloudSnapshot {
        requestRefresh()
        return snapshot
    }

    override fun requestRefresh() {
        if (isStale(lists)) revalidate(listsMutex, ::refreshLists)
        if (isStale(servers)) revalidate(serversMutex, ::refreshServers)
    }

    override suspend fun groups(): List<Group> = currentLists().groups

    override suspend fun persistentServers(): List<PersistentServer> = currentLists().persistentServers

    override suspend fun servers(): List<Server> {
        val current = servers ?: return awaitServers()
        if (isStale(current)) revalidate(serversMutex, ::refreshServers)

        return current.value
    }

    override suspend fun availableServers(groupName: String): List<Server> = try {
        failFast()
        fetchAvailableServers(groupName)
    } catch (exception: CloudUnavailableException) {
        throw exception
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        CloudOutageLog.failed(exception)
        throw CloudUnavailableException(exception)
    }

    suspend fun warmUp() {
        ignoringOutage { currentLists() }
        ignoringOutage { servers() }
    }

    suspend fun refreshNow() {
        if (!mayProbe()) return
        if (listsMutex.withLock { refreshLists(force = true) }) serversMutex.withLock { refreshServers(force = true) }
    }

    fun shutdown() {
        ownScope?.cancel()
    }

    private suspend fun currentLists(): Lists {
        val current = lists ?: return awaitLists()
        if (isStale(current)) revalidate(listsMutex, ::refreshLists)

        return current.value
    }

    private suspend fun awaitLists(): Lists = listsMutex.withLock {
        lists?.let { return it.value }
        failFast()
        try {
            val fetched = Lists(fetchGroups(), fetchPersistentServers())
            storeLists(Cached(fetched, now()))
            recovered()
            fetched
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw coldFailure(exception)
        }
    }

    private suspend fun awaitServers(): List<Server> = serversMutex.withLock {
        servers?.let { return it.value }
        failFast()
        try {
            val fetched = fetchServers()
            storeServers(Cached(fetched, now()))
            recovered()
            fetched
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            throw coldFailure(exception)
        }
    }

    private suspend fun refreshLists(force: Boolean = false): Boolean {
        val stale = lists
        if (!force && !isStale(stale)) return true

        return try {
            storeLists(Cached(Lists(fetchGroups(), fetchPersistentServers()), now()))
            recovered()
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            backgroundFailure(exception, stale) { storeLists(Cached(it.value, now())) }
            false
        }
    }

    private suspend fun refreshServers(force: Boolean = false): Boolean {
        val stale = servers
        if (!force && !isStale(stale)) return true

        return try {
            storeServers(Cached(fetchServers(), now()))
            recovered()
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            backgroundFailure(exception, stale) { storeServers(Cached(it.value, now())) }
            false
        }
    }

    private fun revalidate(mutex: Mutex, refresh: suspend () -> Boolean) {
        if (!mayProbe()) return
        if (!mutex.tryLock()) return
        launchTask {
            try {
                refresh()
            } finally {
                mutex.unlock()
            }
        }
    }

    private fun storeLists(value: Cached<Lists>) {
        lists = value
        publish()
    }

    private fun storeServers(value: Cached<List<Server>>) {
        servers = value
        publish()
    }

    @Synchronized
    private fun publish() {
        val lists = this.lists
        val servers = this.servers
        snapshot = CloudSnapshot(
            groups = lists?.value?.groups.orEmpty(),
            persistentServers = lists?.value?.persistentServers.orEmpty(),
            servers = servers?.value.orEmpty(),
            known = lists != null && servers != null,
        )
    }

    private fun recovered() {
        lastFailure = null
        CloudOutageLog.recovered()
    }

    private fun <T> backgroundFailure(exception: Exception, stale: Cached<T>?, renew: (Cached<T>) -> Unit) {
        CloudOutageLog.failed(exception)
        lastFailure = now() to exception
        stale?.let(renew)
    }

    private fun coldFailure(exception: Exception): CloudUnavailableException {
        CloudOutageLog.failed(exception)
        lastFailure = now() to exception

        return CloudUnavailableException(exception)
    }

    private fun failFast() {
        val (at, cause) = lastFailure ?: return
        if (now() - at <= ttlMillis) throw CloudUnavailableException(cause)
    }

    private fun mayProbe(): Boolean {
        val (at, _) = lastFailure ?: return true
        return now() - at > ttlMillis
    }

    private fun isStale(cached: Cached<*>?): Boolean = cached == null || now() - cached.fetchedAt > ttlMillis

    private inline fun ignoringOutage(read: () -> Unit) = try {
        read()
    } catch (_: CloudUnavailableException) {
    }

    private companion object {
        const val TTL_MILLIS = 2_500L
    }
}
