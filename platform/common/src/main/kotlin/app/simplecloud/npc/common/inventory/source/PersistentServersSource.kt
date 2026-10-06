package app.simplecloud.npc.common.inventory.source

import app.simplecloud.npc.core.cloud.CloudListCache
import app.simplecloud.npc.core.cloud.ServerBridge
import app.simplecloud.npc.core.cloud.isProxy
import app.simplecloud.npc.core.cloud.serversOf
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.EntryStates

object PersistentServersSource : InventoryDataSource {

    override val type = "persistent-servers"
    override val defaultEntryActions = listOf(NpcConfig.ActionConfiguration(sendToServer = "<entry_server>"))

    override suspend fun entries(
        source: InventoryConfiguration.SourceConfiguration,
        context: InventoryOpenContext,
    ): List<InventoryEntry> {
        val servers = CloudListCache.servers()
        return CloudListCache.persistentServers()
            .filterNot { it.isProxy() }
            .sortedBy { it.name.lowercase() }
            .map { persistentServer ->
                val bridge = ServerBridge.OfPersistentServer(persistentServer)
                InventoryEntry(
                    key = "pserver:${persistentServer.name}",
                    placeholders = bridge,
                    substitutions = mapOf("<entry_server>" to persistentServer.name),
                    state = EntryStates.of(serversOf(bridge, servers)),
                )
            }
    }
}
