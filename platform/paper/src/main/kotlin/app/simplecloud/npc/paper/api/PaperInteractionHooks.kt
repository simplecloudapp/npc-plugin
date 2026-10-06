package app.simplecloud.npc.paper.api

import app.simplecloud.npc.api.event.MenuClickEvent
import app.simplecloud.npc.api.event.NpcInteractEvent
import app.simplecloud.npc.api.event.NpcJoinEvent
import app.simplecloud.npc.common.platform.InteractionHooks
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.platform.NpcPlayer
import org.bukkit.Bukkit
import org.bukkit.event.Event

object PaperInteractionHooks : InteractionHooks {

    override fun interact(
        npcId: String,
        player: NpcPlayer,
        interaction: PlayerInteraction,
        joinState: String,
    ): Boolean {
        val bukkit = Bukkit.getPlayer(player.uniqueId) ?: return true
        val event = fire(NpcInteractEvent(bukkit, npcId, interaction.name, joinState, !Bukkit.isPrimaryThread()))

        return !event.isCancelled
    }

    override fun join(npcId: String?, player: NpcPlayer, destination: String): String? {
        val bukkit = Bukkit.getPlayer(player.uniqueId) ?: return destination
        val event = fire(NpcJoinEvent(bukkit, npcId, destination, !Bukkit.isPrimaryThread()))

        return if (event.isCancelled) null else event.destination
    }

    override fun menuClick(menuId: String, slot: Int, player: NpcPlayer, click: PlayerInteraction): Boolean {
        val bukkit = Bukkit.getPlayer(player.uniqueId) ?: return true
        val event = fire(MenuClickEvent(bukkit, menuId, slot, click.name, !Bukkit.isPrimaryThread()))

        return !event.isCancelled
    }

    private fun <E : Event> fire(event: E): E = event.also(Bukkit.getPluginManager()::callEvent)
}
