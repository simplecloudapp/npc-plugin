package app.simplecloud.npc.core.cloud

import app.simplecloud.api.group.Group
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.server.Server

sealed interface ServerBridge {
    val name: String
    val properties: Map<String, Any>

    data class OfGroup(val group: Group) : ServerBridge {
        override val name: String get() = group.name
        override val properties: Map<String, Any> get() = group.properties
    }

    data class OfPersistentServer(val persistentServer: PersistentServer) : ServerBridge {
        override val name: String get() = persistentServer.name
        override val properties: Map<String, Any> get() = persistentServer.properties
    }

    data class OfServer(val server: Server) : ServerBridge {
        override val name: String get() = server.connectName() ?: server.serverId
        override val properties: Map<String, Any> get() = server.properties
    }
}

object ServerBridgeResolver {
    private val SERVER_NAME = Regex("^(.+)-(\\d+)$")

    fun resolve(name: String, snapshot: CloudSnapshot): ServerBridge? =
        when (val resolution = TargetResolver.resolve(name, snapshot, allowProxy = true)) {
            is TargetResolution.Found -> resolution.target.bridge
            TargetResolution.Ambiguous -> null
            TargetResolution.NotFound -> serverByName(name, snapshot.servers)?.let(ServerBridge::OfServer)
        }

    suspend fun resolve(name: String, lists: CloudLists = CloudListCache): ServerBridge? =
        when (val resolution = TargetResolver.resolve(name, lists, allowProxy = true)) {
            is TargetResolution.Found -> resolution.target.bridge
            TargetResolution.Ambiguous -> null
            TargetResolution.NotFound -> serverByName(name, lists.servers())?.let(ServerBridge::OfServer)
        }

    private fun serverByName(name: String, servers: List<Server>): Server? {
        val (groupName, id) = SERVER_NAME.matchEntire(name)?.destructured ?: return null
        val numericalId = id.toIntOrNull() ?: return null

        return servers.firstOrNull { it.groupName().equals(groupName, true) && it.numericalId == numericalId }
    }
}
