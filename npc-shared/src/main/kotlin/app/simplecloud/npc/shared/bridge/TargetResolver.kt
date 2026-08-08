package app.simplecloud.npc.shared.bridge

import app.simplecloud.npc.shared.cloud.CloudService
import kotlinx.coroutines.future.await

enum class TargetType {
    GROUP,
    PERSISTENT_SERVER,
}

data class ResolvedTarget(
    val name: String,
    val type: TargetType,
)

sealed interface TargetResolution {
    data class Found(val target: ResolvedTarget) : TargetResolution
    data object NotFound : TargetResolution
    data object Ambiguous : TargetResolution
}

object TargetResolver {
    suspend fun resolve(name: String): TargetResolution {
        val groups = CloudService.cloudApi.group().allGroups.await()
            .filter { it.name.equals(name, true) }
        val persistentServers = CloudService.cloudApi.persistentServer().allPersistentServers.await()
            .filter { it.name.equals(name, true) }

        return when {
            groups.isNotEmpty() && persistentServers.isNotEmpty() -> TargetResolution.Ambiguous
            groups.isNotEmpty() -> TargetResolution.Found(ResolvedTarget(groups.first().name, TargetType.GROUP))
            persistentServers.isNotEmpty() -> TargetResolution.Found(
                ResolvedTarget(persistentServers.first().name, TargetType.PERSISTENT_SERVER)
            )
            else -> TargetResolution.NotFound
        }
    }

    suspend fun suggestions(): List<String> = buildList {
        addAll(CloudService.cloudApi.group().allGroups.await().map { it.name })
        addAll(CloudService.cloudApi.persistentServer().allPersistentServers.await().map { it.name })
    }.distinct().sorted()
}
