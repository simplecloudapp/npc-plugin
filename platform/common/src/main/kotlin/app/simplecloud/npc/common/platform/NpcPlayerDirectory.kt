package app.simplecloud.npc.common.platform

import app.simplecloud.npc.core.platform.NpcPlayer

interface NpcPlayerDirectory {
    fun findOnlinePlayer(name: String): NpcPlayer?
    fun onlinePlayers(): List<NpcPlayer>
}
