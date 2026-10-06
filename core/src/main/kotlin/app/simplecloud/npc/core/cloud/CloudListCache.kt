package app.simplecloud.npc.core.cloud

import app.simplecloud.api.CloudApi
import app.simplecloud.api.group.Group
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerQuery
import app.simplecloud.api.server.ServerState
import kotlinx.coroutines.future.await

object CloudListCache : CloudLists, CloudSnapshots {

    val cloudApi: CloudApi by lazy { CloudApi.create() }

    private val store: CloudListStore by lazy {
        CloudListStore(
            fetchGroups = { cloudApi.group().allGroups.await().toList() },
            fetchPersistentServers = { cloudApi.persistentServer().allPersistentServers.await().toList() },
            fetchServers = { cloudApi.server().allServers.await() },
            fetchAvailableServers = { groupName ->
                val query = ServerQuery.create()
                    .filterByServerGroupName(groupName)
                    .filterByState(ServerState.AVAILABLE)
                cloudApi.server().getAllServers(query).await()
            },
        )
    }

    override fun peek(): CloudSnapshot = store.peek()

    override fun requestRefresh() = store.requestRefresh()

    override suspend fun groups(): List<Group> = store.groups()

    override suspend fun persistentServers(): List<PersistentServer> = store.persistentServers()

    override suspend fun servers(): List<Server> = store.servers()

    override suspend fun availableServers(groupName: String): List<Server> = store.availableServers(groupName)

    suspend fun warmUp() = store.warmUp()

    suspend fun refreshNow() = store.refreshNow()

    fun shutdown() = store.shutdown()
}
