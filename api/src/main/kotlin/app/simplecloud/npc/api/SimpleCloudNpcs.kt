package app.simplecloud.npc.api

import org.bukkit.entity.Player

interface SimpleCloudNpcs {
    fun npcIds(): Set<String>
    fun menuIds(): Set<String>
    fun openMenu(player: Player, menuId: String): Boolean
}
