package app.simplecloud.npc.common.inventory.source

import app.simplecloud.npc.core.config.JoinStrategy

data class InventoryOpenContext(
    val targetServers: List<String> = emptyList(),
    val joinStrategy: JoinStrategy = JoinStrategy.LEAST_PLAYERS,
)
