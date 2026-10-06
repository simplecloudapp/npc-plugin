package app.simplecloud.npc.paper.pushback

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.repository.NpcRepository
import app.simplecloud.npc.paper.sound.SoundPlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.util.Vector
import java.util.UUID

class PushbackListener(
    private val npcRepository: NpcRepository,
) : Listener {

    private val cooldowns = hashMapOf<UUID, Long>()

    @Volatile
    private var pushbackNpcs: List<NpcConfig> = emptyList()

    init {
        npcRepository.onAnyChange(::rebuildSnapshot)
        rebuildSnapshot()
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onMove(event: PlayerMoveEvent) {
        val npcs = pushbackNpcs
        if (npcs.isEmpty()) return

        val destination = event.to
        val from = event.from
        if (from.x == destination.x && from.y == destination.y && from.z == destination.z) return

        val player = event.player
        val now = System.currentTimeMillis()
        if ((cooldowns[player.uniqueId] ?: 0L) > now) return

        val destinationWorld = destination.world ?: return
        val config = npcs.firstOrNull { candidate ->
            val location = candidate.entity.location
            val dx = location.x - destination.x
            val dy = location.y - destination.y
            val dz = location.z - destination.z
            val radius = candidate.pushback.radius
            val outOfRange = dx * dx + dy * dy + dz * dz > radius * radius
            destinationWorld.name.equals(location.world, ignoreCase = true) && !outOfRange
        } ?: return

        val location = config.entity.location
        Pushback.push(
            player,
            destination.toVector(),
            destination.yaw,
            Vector(location.x, location.y, location.z),
            config.pushback.strength,
            config.pushback.vertical,
        )

        config.pushback.sound?.let { SoundPlayer.play(player, it, config.pushback.soundOptions) }
        cooldowns[player.uniqueId] = now + COOLDOWN_MILLIS
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        cooldowns.remove(event.player.uniqueId)
    }

    private fun rebuildSnapshot() {
        pushbackNpcs = npcRepository.findAll().filter { it.pushback.enabled && !it.entity.needsRelocation }
    }

    private companion object {
        const val COOLDOWN_MILLIS = 500L
    }
}
