package app.simplecloud.npc.core.cloud

import app.simplecloud.api.group.Group
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Proxy

object CloudFakes {
    fun group(
        name: String,
        type: GroupServerType = GroupServerType.SERVER,
        properties: Map<String, Any> = emptyMap(),
    ): Group = fake(Group::class.java, mapOf("getName" to name, "getType" to type, "getProperties" to properties))

    fun persistentServer(
        name: String,
        type: GroupServerType = GroupServerType.SERVER,
        properties: Map<String, Any> = emptyMap(),
    ): PersistentServer = fake(
        PersistentServer::class.java,
        mapOf(
            "getName" to name,
            "getType" to type,
            "getProperties" to properties,
            "getPersistentServerId" to "ps-$name",
        ),
    )

    fun server(
        group: Group,
        numericalId: Int,
        state: ServerState = ServerState.AVAILABLE,
        playerCount: Int = 0,
        maxPlayers: Int? = null,
    ): Server = fake(
        Server::class.java,
        mapOf(
            "getGroup" to group,
            "getServerBase" to group,
            "getServerId" to "${group.name}-$numericalId",
            "getNumericalId" to numericalId,
            "getState" to state,
            "getPlayerCount" to playerCount,
            "getMaxPlayers" to maxPlayers,
            "getProperties" to emptyMap<String, Any>(),
        ),
    )

    fun server(
        persistentServer: PersistentServer,
        state: ServerState = ServerState.AVAILABLE,
        playerCount: Int = 0,
        maxPlayers: Int? = null,
    ): Server = fake(
        Server::class.java,
        mapOf(
            "getPersistentServer" to persistentServer,
            "getPersistentServerId" to persistentServer.persistentServerId,
            "getServerBase" to persistentServer,
            "getServerId" to persistentServer.name,
            "getState" to state,
            "getPlayerCount" to playerCount,
            "getMaxPlayers" to maxPlayers,
            "getProperties" to emptyMap<String, Any>(),
        ),
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T> fake(type: Class<T>, values: Map<String, Any?>): T = Proxy.newProxyInstance(
        type.classLoader,
        arrayOf(type),
    ) { proxy, method, args ->
        when (method.name) {
            "equals" -> proxy === args?.get(0)
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "${type.simpleName}(${values["getName"] ?: values["getServerId"]})"
            in values -> values[method.name]
            else -> when (method.returnType) {
                Int::class.javaPrimitiveType -> 0
                Boolean::class.javaPrimitiveType -> false
                else -> null
            }
        }
    } as T
}

class FakeCloudLists(
    private val groups: List<Group> = emptyList(),
    private val persistentServers: List<PersistentServer> = emptyList(),
    private val servers: List<Server> = emptyList(),
) : CloudLists {
    override suspend fun groups(): List<Group> = groups
    override suspend fun persistentServers(): List<PersistentServer> = persistentServers
    override suspend fun servers(): List<Server> = servers
    override suspend fun availableServers(groupName: String): List<Server> =
        servers.filter { it.groupName().equals(groupName, true) && it.state == ServerState.AVAILABLE }
}

object UnreachableCloud : CloudLists {
    private fun down(): Nothing = throw CloudUnavailableException(IllegalStateException("controller offline"))
    override suspend fun groups(): List<Group> = down()
    override suspend fun persistentServers(): List<PersistentServer> = down()
    override suspend fun servers(): List<Server> = down()
    override suspend fun availableServers(groupName: String): List<Server> = down()
}

fun FakeCloudLists.asSnapshot(): CloudSnapshot = runBlocking {
    CloudSnapshot(groups(), persistentServers(), servers(), known = true)
}
