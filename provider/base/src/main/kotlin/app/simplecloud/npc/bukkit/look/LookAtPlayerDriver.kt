package app.simplecloud.npc.bukkit.look

import app.simplecloud.npc.core.NpcLog
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.logging.Level

class LookAtPlayerDriver(
    private val plugin: Plugin,
    private val ticker: LookAtPlayerTicker,
) {

    class Target(
        val npcId: String,
        val entityId: Int,
        val x: Double,
        val y: Double,
        val z: Double,
        val viewers: Iterable<Player>,
        val maxDistance: Double,
    )

    private var task: BukkitTask? = null
    private val quitListener = object : Listener {
        @EventHandler
        fun onQuit(event: PlayerQuitEvent) = ticker.forgetViewer(event.player.uniqueId)
    }

    fun start(targets: () -> Iterable<Target>) {
        stop()
        Bukkit.getPluginManager().registerEvents(quitListener, plugin)
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { tick(targets) }, INITIAL_DELAY, PERIOD)
    }

    fun stop() {
        task?.cancel()
        task = null
        HandlerList.unregisterAll(quitListener)
        ticker.clear()
    }

    private fun tick(targets: () -> Iterable<Target>) {
        val current = try {
            targets()
        } catch (exception: Exception) {
            logger.log(Level.WARNING, "Failed to collect the look-at-player targets", exception)
            return
        }

        current.forEach { target ->
            try {
                ticker.tick(target.entityId, target.x, target.y, target.z, target.viewers, target.maxDistance)
            } catch (exception: Exception) {
                logger.log(Level.WARNING, "Failed to update look-at-player for NPC ${target.npcId}", exception)
            }
        }
    }

    private companion object {
        private const val PERIOD = 2L
        private const val INITIAL_DELAY = 20L
        private val logger = NpcLog.logger
    }
}
