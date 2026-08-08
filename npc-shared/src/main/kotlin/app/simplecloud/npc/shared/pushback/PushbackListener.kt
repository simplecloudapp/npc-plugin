package app.simplecloud.npc.shared.pushback

import app.simplecloud.npc.shared.namespace.NpcNamespace
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.util.Vector
import java.util.UUID

class PushbackListener(
    private val namespace: NpcNamespace,
) : Listener {
    private val cooldowns = hashMapOf<UUID, Long>()

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        val destination = event.to
        if (event.from.x == destination.x && event.from.y == destination.y && event.from.z == destination.z) return

        val player = event.player
        val now = System.currentTimeMillis()
        if ((cooldowns[player.uniqueId] ?: 0L) > now) return

        namespace.npcRepository.findAll()
            .asSequence()
            .filter { it.pushback.enabled }
            .forEach { config ->
                val npcLocation = namespace.findLocationByNpc(config.id) ?: return@forEach
                if (npcLocation.world != destination.world) return@forEach
                val radius = config.pushback.radius
                if (npcLocation.distanceSquared(destination) > radius * radius) return@forEach

                push(player, npcLocation.toVector(), config.pushback.strength, config.pushback.vertical)
                config.pushback.sound?.let { playSound(player, it) }
                cooldowns[player.uniqueId] = now + COOLDOWN_MILLIS
                return
            }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        cooldowns.remove(event.player.uniqueId)
    }

    private fun push(player: Player, npc: Vector, strength: Double, vertical: Double) {
        val direction = player.location.toVector().subtract(npc).setY(0)
        if (direction.lengthSquared() < 0.0001) {
            direction.copy(player.location.direction.multiply(-1).setY(0))
        }
        player.velocity = direction.normalize().multiply(strength).setY(vertical)
    }

    private fun playSound(player: Player, sound: String) {
        runCatching {
            player.playSound(Sound.sound(Key.key(sound), Sound.Source.MASTER, 1F, 1F))
        }
    }

    companion object {
        private const val COOLDOWN_MILLIS = 500L
    }
}
