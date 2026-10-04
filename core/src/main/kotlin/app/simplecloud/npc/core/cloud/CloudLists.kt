package app.simplecloud.npc.core.cloud

import app.simplecloud.api.group.Group
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.server.Server

interface CloudLists {
    suspend fun groups(): List<Group>
    suspend fun persistentServers(): List<PersistentServer>
    suspend fun servers(): List<Server>

    suspend fun availableServers(groupName: String): List<Server>
}
