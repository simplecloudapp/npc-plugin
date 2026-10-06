package app.simplecloud.npc.common.inventory.source

import app.simplecloud.npc.core.cloud.CloudListCache
import app.simplecloud.npc.core.cloud.ServerBridge
import app.simplecloud.npc.core.cloud.isProxy
import app.simplecloud.npc.core.cloud.serversOf
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.EntryStates

object GroupsSource : InventoryDataSource {

    override val type = "groups"
    override val defaultEntryActions = listOf(NpcConfig.ActionConfiguration(joinTarget = true))

    override suspend fun entries(
        source: InventoryConfiguration.SourceConfiguration,
        context: InventoryOpenContext,
    ): List<InventoryEntry> {
        val servers = CloudListCache.servers()
        return CloudListCache.groups()
            .filterNot { it.isProxy() }
            .sortedBy { it.name.lowercase() }
            .map { group ->
                val bridge = ServerBridge.OfGroup(group)
                InventoryEntry(
                    key = "group:${group.name}",
                    placeholders = bridge,
                    state = EntryStates.of(serversOf(bridge, servers)),
                    substitutions = mapOf("<entry_group>" to group.name),
                    targetOverride = listOf(group.name),
                )
            }
    }
}
