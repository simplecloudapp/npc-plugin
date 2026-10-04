package app.simplecloud.npc.common.inventory.source

import app.simplecloud.npc.core.cloud.ServerBridge
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration

class InventoryEntry(
    val key: String,
    val placeholders: ServerBridge? = null,
    val substitutions: Map<String, String> = emptyMap(),
    val targetOverride: List<String> = emptyList(),
    val overrideIcon: InventoryConfiguration.InventoryItemConfiguration? = null,
    val overrideActions: List<NpcConfig.ActionConfiguration>? = null,
    val state: String? = null,
)
