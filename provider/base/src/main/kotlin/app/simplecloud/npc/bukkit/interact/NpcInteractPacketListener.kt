package app.simplecloud.npc.bukkit.interact

import app.simplecloud.npc.core.interaction.PlayerInteraction
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin

class NpcInteractPacketListener(
    private val plugin: Plugin,
    private val onInteract: OnNpcInteract,
) : PacketListenerAbstract(), Listener {

    private val dedupe = ClickDedupeTracker<Pair<Int, PlayerInteraction>>()

    override fun onPacketReceive(event: PacketReceiveEvent) {
        val click = ClickPackets.read(event) ?: return
        val target = InteractableEntities.find(click.entityId) ?: return
        if (target.realEntity) event.isCancelled = true

        val interaction = click.interaction ?: return
        val player = event.getPlayer<Player>()
        if (!dedupe.shouldHandle(player.uniqueId, click.entityId to interaction)) return

        Bukkit.getScheduler().runTask(
            plugin,
            Runnable { if (player.isOnline) onInteract(target.npcId, player, interaction) },
        )
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        dedupe.forgetPlayer(event.player.uniqueId)
    }
}
