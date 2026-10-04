package app.simplecloud.npc.core.cloud

import app.simplecloud.api.group.Group
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState

class CloudSnapshot(
    val groups: List<Group> = emptyList(),
    val persistentServers: List<PersistentServer> = emptyList(),
    val servers: List<Server> = emptyList(),
    val known: Boolean = false,
) : CloudLists {

    override suspend fun groups(): List<Group> = groups
    override suspend fun persistentServers(): List<PersistentServer> = persistentServers
    override suspend fun servers(): List<Server> = servers

    override suspend fun availableServers(groupName: String): List<Server> = servers.filter {
        it.groupName().equals(groupName, true) && it.state == ServerState.AVAILABLE
    }

    companion object {
        val EMPTY = CloudSnapshot()
    }
}
