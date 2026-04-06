package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.bridge.ServerBridgeFinder
import app.simplecloud.npc.shared.config.NpcConfig

/**
 * @author Niklas Nieberler
 */

object JoinStateHelper {

    suspend fun getJoinState(config: NpcConfig): String? {
        val serverBaseName = config.hologramConfiguration.placeholderServerBaseName
        return getJoinState(serverBaseName)
    }

    suspend fun getJoinState(baseName: String): String? {
        val serverBridge = ServerBridgeFinder.find(baseName)
        return serverBridge?.properties["joinstate"] as String?
    }

}