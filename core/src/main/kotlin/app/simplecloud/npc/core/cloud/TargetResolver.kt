package app.simplecloud.npc.core.cloud

import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import app.simplecloud.npc.core.config.JoinStrategy

enum class TargetType {
    GROUP,
    PERSISTENT_SERVER,
}

data class ResolvedTarget(
    val name: String,
    val type: TargetType,
    val bridge: ServerBridge,
)

sealed interface TargetResolution {
    data class Found(val target: ResolvedTarget) : TargetResolution
    data object NotFound : TargetResolution
    data object Ambiguous : TargetResolution
}

object TargetResolver {
    fun resolve(
        name: String,
        snapshot: CloudSnapshot,
        allowProxy: Boolean = false,
    ): TargetResolution {
        val groups = snapshot.groups.filter { it.name.equals(name, true) && (allowProxy || !it.isProxy()) }
        val persistentServers =
            snapshot.persistentServers.filter { it.name.equals(name, true) && (allowProxy || !it.isProxy()) }

        return when {
            groups.isNotEmpty() && persistentServers.isNotEmpty() -> TargetResolution.Ambiguous
            groups.isNotEmpty() -> groups.first().let {
                TargetResolution.Found(ResolvedTarget(it.name, TargetType.GROUP, ServerBridge.OfGroup(it)))
            }

            persistentServers.isNotEmpty() -> persistentServers.first().let {
                TargetResolution.Found(
                    ResolvedTarget(it.name, TargetType.PERSISTENT_SERVER, ServerBridge.OfPersistentServer(it)),
                )
            }

            else -> TargetResolution.NotFound
        }
    }

    suspend fun resolve(
        name: String,
        lists: CloudLists = CloudListCache,
        allowProxy: Boolean = false,
    ): TargetResolution = resolve(
        name,
        lists as? CloudSnapshot ?: CloudSnapshot(lists.groups(), lists.persistentServers(), known = true),
        allowProxy,
    )

    suspend fun findDestination(
        targetName: String,
        lists: CloudLists = CloudListCache,
        strategy: JoinStrategy = JoinStrategy.LEAST_PLAYERS,
    ): String? {
        val target = (resolve(targetName, lists) as? TargetResolution.Found)?.target ?: return null

        return when (target.type) {
            TargetType.PERSISTENT_SERVER -> target.name.takeIf {
                serversOf(target.bridge, lists.servers()).any { it.state == ServerState.AVAILABLE }
            }
            TargetType.GROUP -> {
                val available = lists.availableServers(target.name)
                pick(available.filterNot(Server::isFull).ifEmpty { available }, strategy)?.connectName()
            }
        }
    }

    private fun pick(servers: List<Server>, strategy: JoinStrategy): Server? = when (strategy) {
        JoinStrategy.LEAST_PLAYERS -> servers.minByOrNull { it.playerCount ?: 0 }
        JoinStrategy.MOST_PLAYERS -> servers.maxByOrNull { it.playerCount ?: 0 }
        JoinStrategy.RANDOM -> servers.randomOrNull()
    }
}
