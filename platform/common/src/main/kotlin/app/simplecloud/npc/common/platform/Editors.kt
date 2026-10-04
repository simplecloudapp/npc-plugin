package app.simplecloud.npc.common.platform

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer

interface Editors {
    fun openNpc(player: NpcPlayer, config: NpcConfig)
    fun openInventory(player: NpcPlayer, config: InventoryConfiguration)
    fun openInventoryCreate(player: NpcPlayer)
    fun refreshOpenEditors(players: List<NpcPlayer>) = Unit

    companion object {
        val NOOP: Editors = object : Editors {
            override fun openNpc(player: NpcPlayer, config: NpcConfig) = Unit
            override fun openInventory(player: NpcPlayer, config: InventoryConfiguration) = Unit
            override fun openInventoryCreate(player: NpcPlayer) = Unit
        }
    }
}
