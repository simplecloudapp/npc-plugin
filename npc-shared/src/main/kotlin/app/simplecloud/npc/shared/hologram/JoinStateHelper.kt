package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.controller.ControllerService

/**
 * @author Niklas Nieberler
 */

object JoinStateHelper {

    suspend fun getJoinState(config: NpcConfig): String? {
        val groupName = config.hologramConfiguration.placeholderGroupName
        return getJoinState(groupName)
    }

    suspend fun getJoinState(groupName: String): String? {
        val group = ControllerService.controllerApi.getGroups().getGroupByName(groupName)
        return group.properties["joinstate"]
    }

}