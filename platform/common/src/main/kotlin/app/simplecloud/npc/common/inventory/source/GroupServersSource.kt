package app.simplecloud.npc.common.inventory.source

import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import app.simplecloud.npc.core.cloud.CloudListCache
import app.simplecloud.npc.core.cloud.ServerBridge
import app.simplecloud.npc.core.cloud.TargetResolution
import app.simplecloud.npc.core.cloud.TargetResolver
import app.simplecloud.npc.core.cloud.TargetType
import app.simplecloud.npc.core.cloud.connectName
import app.simplecloud.npc.core.cloud.groupName
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.EntryStates

object GroupServersSource : InventoryDataSource {

    const val TYPE = InventoryConfiguration.SourceConfiguration.DEFAULT_TYPE
    const val TARGET_GROUP = "<target>"
    override val type = TYPE
    override val defaultEntryActions = listOf(NpcConfig.ActionConfiguration(sendToServer = "<entry_server>"))

    override suspend fun entries(
        source: InventoryConfiguration.SourceConfiguration,
        context: InventoryOpenContext,
    ): List<InventoryEntry> {
        val groupName = resolveGroupName(source, context) ?: return emptyList()
        val states = source.states.mapNotNull { runCatching { ServerState.valueOf(it.uppercase()) }.getOrNull() }

        return CloudListCache.servers()
            .asSequence()
            .filter { it.groupName().equals(groupName, ignoreCase = true) }
            .filter { server ->
                if (states.isNotEmpty()) server.state in states
                else server.state != ServerState.STOPPING && server.state != ServerState.CLEANUP
            }
            .sortedWith(comparatorFor(source.sort))
            .mapNotNull(::toEntry)
            .toList()
    }

    private suspend fun resolveGroupName(
        source: InventoryConfiguration.SourceConfiguration,
        context: InventoryOpenContext,
    ): String? {
        source.group?.takeIf { it.isNotBlank() && it != TARGET_GROUP }?.let { return it }

        val target = context.targetServers.firstOrNull() ?: return null
        val resolution = TargetResolver.resolve(target) as? TargetResolution.Found ?: return null

        return resolution.target.name.takeIf { resolution.target.type == TargetType.GROUP }
    }

    private fun comparatorFor(sort: String?): Comparator<Server> = when (sort?.uppercase()) {
        "PLAYER_COUNT" -> compareByDescending<Server> { it.playerCount ?: 0 }.thenBy { it.numericalId }
        else -> compareBy { it.numericalId }
    }

    private fun toEntry(server: Server): InventoryEntry? {
        val connectName = server.connectName() ?: return null

        return InventoryEntry(
            key = "server:$connectName",
            placeholders = ServerBridge.OfServer(server),
            state = EntryStates.of(server),
            substitutions = mapOf(
                "<entry_server>" to connectName,
                "<entry_group>" to server.groupName().orEmpty(),
                "<entry_numerical_id>" to server.numericalId.toString(),
            ),
        )
    }
}
