package app.simplecloud.npc.bukkit.hologram

import app.simplecloud.npc.bukkit.packet.PacketComponents
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.cloud.CloudOutageLog
import app.simplecloud.npc.core.hologram.PersonalText
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import kotlin.math.roundToInt
import app.simplecloud.npc.relocate.packets.kyori.adventure.text.Component as PacketComponent

data class HologramLineKey(val npcId: String, val index: Int) {
    companion object {
        fun of(npcId: String, index: Int) = HologramLineKey(npcId.lowercase(Locale.ROOT), index)
    }
}

class PersonalLine(val text: PersonalText, val refreshSeconds: Double)

class ViewerTexts(
    private val plugin: Plugin,
    private val viewersOf: (HologramLineKey) -> Collection<Player>,
    private val push: (HologramLineKey, Player, PacketComponent) -> Unit,
) {

    private class LineState(val key: HologramLineKey, val text: PersonalText, val refreshTicks: Int) {
        val phase: Int = Math.floorMod(key.hashCode(), refreshTicks)
        val components = ConcurrentHashMap<String, PacketComponent>()
    }

    private class Rendered(
        val ticket: Long,
        val source: PersonalText,
        val resolved: String,
        val component: PacketComponent,
    )

    private class Line(@Volatile var state: LineState) {
        val viewers = ConcurrentHashMap<UUID, Rendered>()
    }

    private data class Work(val key: HologramLineKey, val viewer: UUID)

    private val lines = ConcurrentHashMap<HologramLineKey, Line>()
    private val tickets = AtomicLong()

    private val queue = ArrayDeque<Work>()
    private val queued = HashSet<Work>()
    private var tick = 0L
    private var inlineSpent = 0L
    private var task: BukkitTask? = null

    private val worker: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SimpleCloud-NPC hologram text").apply { isDaemon = true }
    }

    fun start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable(::tick), 1L, 1L)
    }

    fun shutdown() {
        task?.cancel()
        task = null
        worker.shutdownNow()
        lines.clear()
    }

    fun track(key: HologramLineKey, personal: PersonalLine?) {
        val existing = lines[key]
        if (personal == null) {
            if (existing != null) lines.remove(key, existing)
            return
        }

        val refreshTicks = (personal.refreshSeconds * TICKS_PER_SECOND).roundToInt().coerceAtLeast(MIN_REFRESH_TICKS)
        val previous = existing?.state
        if (previous != null && previous.text == personal.text && previous.refreshTicks == refreshTicks) return

        val state = LineState(key, personal.text, refreshTicks)
        val target = existing ?: Line(state).also { lines[key] = it }
        target.state = state
        if (previous?.text == personal.text) return

        viewersOf(key).forEach { player ->
            if (state.text.external) enqueue(Work(key, player.uniqueId)) else refresh(target, state, player)
        }
    }

    fun isPersonal(key: HologramLineKey): Boolean = lines.containsKey(key)

    fun textFor(key: HologramLineKey, player: Player): PacketComponent? {
        val line = lines[key] ?: return null
        val state = line.state
        line.viewers[player.uniqueId]?.takeIf { it.source == state.text }?.let { return it.component }
        if (state.text.external && inlineSpent >= BUDGET_NANOS) {
            enqueue(Work(key, player.uniqueId))
            return null
        }

        val started = System.nanoTime()
        val resolved = resolve(state, player)
        val component = runCatching { componentFor(state, resolved) }.getOrNull()
        if (state.text.external) inlineSpent += System.nanoTime() - started
        component ?: return null
        store(line, player.uniqueId, Rendered(tickets.incrementAndGet(), state.text, resolved, component))

        return component
    }

    fun retain(npcId: String, lineCount: Int) {
        val id = npcId.lowercase(Locale.ROOT)
        lines.keys.removeIf { it.npcId == id && it.index >= lineCount }
    }

    fun forget(npcId: String) {
        val id = npcId.lowercase(Locale.ROOT)
        lines.keys.removeIf { it.npcId == id }
    }

    fun viewerGone(npcId: String, viewer: UUID) {
        val id = npcId.lowercase(Locale.ROOT)
        lines.forEach { (key, line) -> if (key.npcId == id) line.viewers.remove(viewer) }
    }

    private fun tick() {
        tick++
        inlineSpent = 0L
        if (lines.isEmpty()) return

        lines.values.forEach { line ->
            val state = line.state
            if (!state.text.external || (tick + state.phase) % state.refreshTicks != 0L) return@forEach
            viewersOf(state.key).forEach { enqueue(Work(state.key, it.uniqueId)) }
        }

        drain()
    }

    private fun enqueue(work: Work) {
        if (queued.add(work)) queue.addLast(work)
    }

    private fun drain() {
        if (queue.isEmpty()) return
        val deadline = System.nanoTime() + BUDGET_NANOS

        do {
            val work = queue.removeFirst()
            queued.remove(work)
            val line = lines[work.key] ?: continue
            val player = Bukkit.getPlayer(work.viewer) ?: continue
            refresh(line, line.state, player)
        } while (queue.isNotEmpty() && System.nanoTime() < deadline)

        if (queue.size > BACKLOG_WARNING) {
            CloudOutageLog.warnThrottled(
                "Hologram placeholders are falling behind (${queue.size} updates waiting). A PlaceholderAPI " +
                    "placeholder in a hologram line is slow; raise that line's refresh interval.",
                key = "hologram-backlog",
            )
        }
    }

    private fun refresh(line: Line, state: LineState, player: Player) {
        val ticket = tickets.incrementAndGet()
        val resolved = resolve(state, player)
        val current = line.viewers[player.uniqueId]
        if (current != null && current.source == state.text && current.resolved == resolved) return

        if (worker.isShutdown) return
        val viewer = player.uniqueId
        worker.execute {
            val component = runCatching { componentFor(state, resolved) }
                .onFailure {
                    NpcLog.logger.log(Level.WARNING, "Could not render a hologram line of '${state.key.npcId}'", it)
                }
                .getOrNull() ?: return@execute
            if (line.state !== state || !store(line, viewer, Rendered(ticket, state.text, resolved, component))) {
                return@execute
            }
            if (!plugin.isEnabled) return@execute
            Bukkit.getScheduler().runTask(plugin, Runnable {
                if (lines[state.key]?.state !== state) return@Runnable
                Bukkit.getPlayer(viewer)?.let { push(state.key, it, component) }
            })
        }
    }

    private fun resolve(state: LineState, player: Player): String {
        if (!state.text.external) return state.text.resolve(player.uniqueId, player.name)
        val started = System.nanoTime()
        val resolved = state.text.resolve(player.uniqueId, player.name)
        val took = System.nanoTime() - started
        if (took > SLOW_NANOS) {
            CloudOutageLog.warnThrottled(
                "A PlaceholderAPI placeholder in hologram line ${state.key.index + 1} of NPC '${state.key.npcId}' " +
                    "took ${took / 1_000_000} ms. Slow placeholders hold up the server; raise its refresh interval.",
                key = "hologram-slow:${state.key}",
            )
        }

        return resolved
    }

    private fun componentFor(state: LineState, resolved: String): PacketComponent {
        if (state.components.size > COMPONENT_CACHE_LIMIT) state.components.clear()

        return state.components.computeIfAbsent(resolved) { PacketComponents.of(state.text.render(it)) }
    }

    private fun store(line: Line, viewer: UUID, rendered: Rendered): Boolean {
        var stored = false
        line.viewers.compute(viewer) { _, current ->
            if (current == null || current.ticket < rendered.ticket) rendered.also { stored = true } else current
        }

        return stored
    }

    private companion object {
        const val TICKS_PER_SECOND = 20
        const val MIN_REFRESH_TICKS = 5
        const val BUDGET_NANOS = 1_500_000L
        const val SLOW_NANOS = 5_000_000L
        const val BACKLOG_WARNING = 2_000
        const val COMPONENT_CACHE_LIMIT = 512
    }
}
