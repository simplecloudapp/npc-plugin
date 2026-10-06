package app.simplecloud.npc.common.inventory.source

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration

object StaticEntriesSource : InventoryDataSource {
    override val type = "static"
    override val defaultEntryActions = emptyList<NpcConfig.ActionConfiguration>()

    override suspend fun entries(
        source: InventoryConfiguration.SourceConfiguration,
        context: InventoryOpenContext,
    ): List<InventoryEntry> = source.entries.mapIndexed { index, item ->
        InventoryEntry(
            key = "static:$index",
            overrideIcon = item,
            overrideActions = item.actions,
        )
    }
}
