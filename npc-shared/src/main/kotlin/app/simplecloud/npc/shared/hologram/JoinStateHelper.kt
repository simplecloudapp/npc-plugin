package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.bridge.ServerBridgeFinder
import app.simplecloud.npc.shared.config.NpcConfig

object JoinStateHelper {
    suspend fun getJoinState(config: NpcConfig): String {
        val target = config.targetServers.firstOrNull() ?: return NpcConfig.DEFAULT_JOIN_STATE
        return getJoinState(target)
    }

    suspend fun getJoinState(target: String): String {
        val serverBridge = ServerBridgeFinder.find(target)
        return ((serverBridge?.properties?.get("join-state")
            ?: serverBridge?.properties?.get("joinstate")) as? String)
            ?.lowercase()
            ?: NpcConfig.DEFAULT_JOIN_STATE
    }
}
