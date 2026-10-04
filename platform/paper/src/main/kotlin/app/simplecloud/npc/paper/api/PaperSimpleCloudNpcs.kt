package app.simplecloud.npc.paper.api

import app.simplecloud.npc.api.SimpleCloudNpcs
import app.simplecloud.npc.common.inventory.source.InventoryOpenContext
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.paper.player.PaperNpcPlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class PaperSimpleCloudNpcs(
    private val plugin: Plugin,
    private val context: () -> NpcPluginContext,
) : SimpleCloudNpcs {

    override fun npcIds(): Set<String> = context().npcRepository.findAll().map { it.id }.toSet()

    override fun menuIds(): Set<String> = context().inventoryRepository.ids().toSet()

    override fun openMenu(player: Player, menuId: String): Boolean {
        val menu = context().inventoryRepository.find(menuId) ?: return false
        context().inventoryViews.open(PaperNpcPlayer(plugin, player), menu.id, InventoryOpenContext(emptyList()))

        return true
    }
}
