package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.cloud.CloudService
import kotlinx.coroutines.future.await

/**
 * @author Niklas Nieberler
 */

object JoinStateHelper {

    suspend fun getJoinState(config: NpcConfig): String? {
        val groupName = config.hologramConfiguration.placeholderGroupName
        return getJoinState(groupName)
    }

    suspend fun getJoinState(groupName: String): String? {
        val group = CloudService.cloudApi.group().getGroupByName(groupName).await()
        return group.properties["joinstate"] as String?
    }

}