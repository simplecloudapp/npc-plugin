package app.simplecloud.npc.shared.action

import app.simplecloud.api.server.ServerQuery
import app.simplecloud.api.server.ServerState
import app.simplecloud.npc.shared.bridge.TargetResolution
import app.simplecloud.npc.shared.bridge.TargetResolver
import app.simplecloud.npc.shared.bridge.TargetType
import app.simplecloud.npc.shared.cloud.CloudService
import kotlinx.coroutines.future.await

object TargetConnectionService {
    suspend fun findDestination(targetName: String): String? {
        val target = (TargetResolver.resolve(targetName) as? TargetResolution.Found)?.target ?: return null
        return when (target.type) {
            TargetType.PERSISTENT_SERVER -> target.name
            TargetType.GROUP -> {
                val query = ServerQuery.create()
                    .filterByServerGroupName(target.name)
                    .filterByState(ServerState.AVAILABLE)
                CloudService.cloudApi.server().getAllServers(query).await()
                    .minByOrNull { it.playerCount }
                    ?.let { "${it.group.name}-${it.numericalId}" }
            }
        }
    }
}
