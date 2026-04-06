package app.simplecloud.npc.shared.bridge

import app.simplecloud.api.server.Server
import app.simplecloud.npc.shared.cloud.CloudService
import app.simplecloud.plugin.api.shared.pattern.ServerPatternIdentifier
import kotlinx.coroutines.future.await

/**
 * @author Niklas Nieberler
 */

object ServerBridgeFinder {

    private val serverPatternIdentifier = ServerPatternIdentifier()
    private val cloudApi = CloudService.cloudApi

    suspend fun find(name: String): ServerBridge? {
        val persistentServer = this.cloudApi.persistentServer().allPersistentServers.await()
                .firstOrNull { it.name == name }
        val group = this.cloudApi.group().allGroups.await()
                .firstOrNull { it.name == name }
        val server = getServerByName(name)

        return when {
            persistentServer != null -> ServerBridge(
                persistentServer.name,
                persistentServer.persistentServerId,
                0,
                persistentServer.properties,
                isServer = false,
                isGroup = false,
                isPersistentServer = true
            )

            group != null -> ServerBridge(
                group.name,
                group.serverGroupId,
                0,
                group.properties,
                isServer = false,
                isGroup = true,
                isPersistentServer = false
            )

            server != null -> ServerBridge(
                server.group.name,
                server.serverGroupId,
                server.numericalId,
                server.properties,
                isServer = true,
                isGroup = false,
                isPersistentServer = false
            )

            else -> null
        }
    }

    private suspend fun getServerByName(name: String): Server? {
        return try {
            val (groupName, numericalId) = this.serverPatternIdentifier.parse(name)
            this.cloudApi.server().allServers.await()
                .firstOrNull { it.group.name == groupName && it.numericalId == numericalId }
        } catch (_: Exception) {
            null
        }
    }
}