package app.simplecloud.npc.common.platform

import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.platform.NpcPlayer

interface InteractionHooks {
    fun interact(npcId: String, player: NpcPlayer, interaction: PlayerInteraction, joinState: String): Boolean = true
    fun join(npcId: String?, player: NpcPlayer, destination: String): String? = destination
    fun menuClick(menuId: String, slot: Int, player: NpcPlayer, click: PlayerInteraction): Boolean = true

    companion object {
        val NONE = object : InteractionHooks {}
    }
}
