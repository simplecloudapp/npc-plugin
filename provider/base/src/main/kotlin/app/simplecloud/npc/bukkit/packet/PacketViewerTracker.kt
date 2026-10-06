package app.simplecloud.npc.bukkit.packet

import app.simplecloud.npc.core.NpcLog
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class PacketViewerTracker(private val plugin: Plugin) : Listener {

    abstract class Entry(
        val label: String,
        @Volatile var world: String,
        @Volatile var x: Double,
        @Volatile var y: Double,
        @Volatile var z: Double,
        @Volatile var range: Double,
    ) {
        val viewers: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

        fun moveTo(world: String, x: Double, y: Double, z: Double) {
            this.world = world
            this.x = x
            this.y = y
            this.z = z
        }

        abstract fun show(player: Player)

        abstract fun hide(player: Player)

        open fun forget(uuid: UUID) = Unit
    }

    private val entries = ConcurrentHashMap.newKeySet<Entry>()
    private val quitHooks = mutableListOf<(UUID) -> Unit>()
    private var task: BukkitTask? = null

    fun start() {
        Bukkit.getPluginManager().registerEvents(this, plugin)
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable(::tick), 20L, TICK_PERIOD)
    }

    fun stop() {
        task?.cancel()
        task = null
        HandlerList.unregisterAll(this)
        entries.clear()
    }

    fun onQuit(hook: (UUID) -> Unit) {
        quitHooks += hook
    }

    fun add(entry: Entry) {
        entries += entry
        Bukkit.getOnlinePlayers().forEach { if (inRange(entry, it)) show(entry, it) }
    }

    fun remove(entry: Entry): List<Player> {
        entries -= entry
        val viewers = entry.viewers.mapNotNull(Bukkit::getPlayer)
        entry.viewers.clear()

        return viewers
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val uuid = event.player.uniqueId
        forget(uuid)
        quitHooks.forEach { it(uuid) }
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) = forget(event.player.uniqueId)

    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) = forget(event.player.uniqueId)

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTeleport(event: PlayerTeleportEvent) {
        if (event.to.world != event.from.world) return
        val player = event.player
        Bukkit.getScheduler().runTask(plugin, Runnable { if (player.isOnline) update(player) })
    }

    private fun forget(uuid: UUID) {
        entries.forEach { entry ->
            if (entry.viewers.remove(uuid)) entry.forget(uuid)
        }
    }

    private fun tick() {
        val players = Bukkit.getOnlinePlayers().toList()

        entries.forEach { entry ->
            try {
                players.forEach { update(entry, it) }
            } catch (exception: Exception) {
                logger.log(Level.WARNING, "Failed to update visibility for ${entry.label}", exception)
            }
        }
    }

    private fun update(player: Player) {
        entries.forEach { entry ->
            try {
                update(entry, player)
            } catch (exception: Exception) {
                logger.log(Level.WARNING, "Failed to update visibility for ${entry.label}", exception)
            }
        }
    }

    private fun update(entry: Entry, player: Player) {
        val withinRange = inRange(entry, player)
        val visible = player.uniqueId in entry.viewers

        when {
            withinRange && !visible -> show(entry, player)
            !withinRange && visible -> hide(entry, player)
        }
    }

    private fun show(entry: Entry, player: Player) {
        if (!entry.viewers.add(player.uniqueId)) return
        entry.show(player)
    }

    private fun hide(entry: Entry, player: Player) {
        if (!entry.viewers.remove(player.uniqueId)) return
        entry.hide(player)
    }

    private fun inRange(entry: Entry, player: Player): Boolean {
        if (!player.world.name.equals(entry.world, ignoreCase = true)) return false
        val location = player.location
        val dx = entry.x - location.x
        val dy = entry.y - location.y
        val dz = entry.z - location.z

        return dx * dx + dy * dy + dz * dz <= entry.range * entry.range
    }

    private companion object {
        const val TICK_PERIOD = 10L
        val logger = NpcLog.logger
    }
}
