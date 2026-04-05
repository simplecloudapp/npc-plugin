package app.simplecloud.npc.shared.utils

import app.simplecloud.api.base.ServerBase
import app.simplecloud.npc.shared.cloud.CloudService
import kotlinx.coroutines.future.await

/**
 * @author Niklas Nieberler
 */

object ServerBaseFinder {

    suspend fun find(name: String): ServerBase? {
        val cloudApi = CloudService.cloudApi
        return cloudApi.persistentServer().getPersistentServerByName(name).await()
            ?: return cloudApi.group().getGroupByName(name).await()
    }

}