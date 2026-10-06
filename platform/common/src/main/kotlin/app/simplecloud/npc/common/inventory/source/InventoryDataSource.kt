package app.simplecloud.npc.common.inventory.source

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration

interface InventoryDataSource {

    val type: String
    val defaultEntryActions: List<NpcConfig.ActionConfiguration>

    suspend fun entries(
        source: InventoryConfiguration.SourceConfiguration,
        context: InventoryOpenContext,
    ): List<InventoryEntry>
}
