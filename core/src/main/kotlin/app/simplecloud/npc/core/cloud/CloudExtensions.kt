package app.simplecloud.npc.core.cloud

import app.simplecloud.api.base.ServerBase
import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.Server

fun ServerBase.isProxy(): Boolean = type == GroupServerType.PROXY
fun Server.groupName(): String? = group?.name
fun Server.connectName(): String? = groupName()?.let { "$it-$numericalId" }

fun Server.isFull(): Boolean {
    val max = maxPlayers ?: return false
    return max > 0 && (playerCount ?: 0) >= max
}

fun serversOf(bridge: ServerBridge, servers: List<Server>): List<Server> = when (bridge) {
    is ServerBridge.OfGroup -> servers.filter { it.groupName().equals(bridge.group.name, true) }
    is ServerBridge.OfPersistentServer -> servers.filter {
        it.persistentServerId != null && it.persistentServerId == bridge.persistentServer.persistentServerId
    }
    is ServerBridge.OfServer -> listOf(bridge.server)
}
