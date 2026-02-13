package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.cloud.CloudService
import app.simplecloud.npc.shared.enums.NpcType
import kotlinx.coroutines.future.await

/**
 * @author Niklas Nieberler
 */

object JoinStateHelper {

    suspend fun getJoinState(config: NpcConfig): String? {
        val placeholderName = config.hologramConfiguration.placeholderName
        return when (config.npcType) {
            NpcType.GROUP -> getJoinState(placeholderName)
            NpcType.PERSISTENT -> null
            null -> null
        }
    }

    suspend fun getJoinState(groupName: String): String? {
        return try {
            val group = CloudService.cloudApi.group().getGroupByName(groupName).await()
            group.properties?.get("joinstate") as String?
        } catch (e: Exception) {
            null
        }
    }

}