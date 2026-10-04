package app.simplecloud.npc.paper.player

import app.simplecloud.npc.common.platform.NpcPlayerDirectory
import app.simplecloud.npc.core.platform.NpcPlayer
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

class PaperPlayerDirectory(
    private val plugin: Plugin,
) : NpcPlayerDirectory {

    override fun findOnlinePlayer(name: String): NpcPlayer? =
        Bukkit.getPlayerExact(name)?.let { PaperNpcPlayer(plugin, it) }

    override fun onlinePlayers(): List<NpcPlayer> = Bukkit.getOnlinePlayers().map { PaperNpcPlayer(plugin, it) }
}
