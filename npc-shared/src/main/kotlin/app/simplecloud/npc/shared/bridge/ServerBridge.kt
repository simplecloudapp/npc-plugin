package app.simplecloud.npc.shared.bridge

import app.simplecloud.api.group.Group
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.server.Server
import app.simplecloud.npc.shared.cloud.CloudService
import kotlinx.coroutines.future.await

/**
 * @author Niklas Nieberler
 */

data class ServerBridge(
    val name: String,
    val serverId: String,
    val numericalId: Int,
    val properties: Map<String, Any>,
    val isServer: Boolean,
    val isGroup: Boolean,
    val isPersistentServer: Boolean
) {

    suspend fun getGroup(): Group? {
        return CloudService.cloudApi.group().getGroupByName(this.name).await()
    }

    suspend fun getPersistentServer(): PersistentServer? {
        return CloudService.cloudApi.persistentServer().getPersistentServerByName(this.name).await()
    }

    suspend fun getServer(): Server? {
        return CloudService.cloudApi.server().allServers.await()
            .firstOrNull { it.group.name == this.name && it.numericalId == this.numericalId }
    }

}