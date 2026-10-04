package app.simplecloud.npc.paper.hologram

import app.simplecloud.npc.bukkit.NpcKeys
import app.simplecloud.npc.bukkit.hologram.HologramLineKey
import app.simplecloud.npc.bukkit.hologram.PersonalLine
import app.simplecloud.npc.bukkit.hologram.ViewerHologramText
import app.simplecloud.npc.bukkit.packet.PacketComponents
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.cloud.CloudOutageLog
import app.simplecloud.npc.core.hologram.PersonalText
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import io.papermc.paper.event.player.PlayerTrackEntityEvent
import io.papermc.paper.event.player.PlayerUntrackEntityEvent
import me.tofaa.entitylib.meta.display.TextDisplayMeta
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Level
import kotlin.math.roundToInt
import app.simplecloud.npc.relocate.packets.kyori.adventure.text.Component as PacketComponent

class PersonalHolograms(private val plugin: Plugin) :
    PacketListenerAbstract(PacketListenerPriority.HIGH),
    ViewerHologramText,
    Listener {

    private class LineState(
        val key: HologramLineKey,
        val text: PersonalText,
        val refreshTicks: Int,
        val entityUuid: UUID,
        val entityId: Int,
    ) {
        val phase: Int = Math.floorMod(key.hashCode(), refreshTicks)
        val components = ConcurrentHashMap<String, PacketComponent>()
    }

    private class Rendered(val ticket: Long, val source: PersonalText, val resolved: String, val component: PacketComponent)

    private class Line(@Volatile var state: LineState) {
        val viewers = ConcurrentHashMap<UUID, Rendered>()
    }

    private data class Work(val key: HologramLineKey, val viewer: UUID)

    private val lines = ConcurrentHashMap<HologramLineKey, Line>()
    private val entities = ConcurrentHashMap<Int, HologramLineKey>()
    private val pending = ConcurrentHashMap.newKeySet<Work>()
    private val tickets = AtomicLong()

    private val queue = ArrayDeque<Work>()
    private val queued = HashSet<Work>()
    private var tick = 0L
    private var task: BukkitTask? = null

    private val worker: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SimpleCloud-NPC hologram text").apply { isDaemon = true }
    }

    @Volatile
    private var textIndex: Int = TextDisplayMeta.OFFSET.toInt()

    fun start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable(::tick), 1L, 1L)
    }

    fun shutdown() {
        task?.cancel()
        task = null
        worker.shutdownNow()
        unbindAll()
    }

    override fun bind(display: TextDisplay, key: HologramLineKey, line: PersonalLine?, shared: Component) {
        val existing = lines[key]
        if (line == null) {
            if (existing == null) return
            forget(key, existing)
            val component = PacketComponents.of(shared)
            display.trackedBy.forEach { push(display.entityId, it, component) }
            return
        }

        val refreshTicks = (line.refreshSeconds * TICKS_PER_SECOND).roundToInt().coerceAtLeast(MIN_REFRESH_TICKS)
        val previous = existing?.state
        val sameText = previous?.text == line.text
        val sameEntity = previous?.entityId == display.entityId
        if (previous != null && sameText && sameEntity && previous.refreshTicks == refreshTicks) return

        val state = LineState(key, line.text, refreshTicks, display.uniqueId, display.entityId)
        val target = existing ?: Line(state).also { lines[key] = it }
        if (previous != null && !sameEntity) entities.remove(previous.entityId, key)
        target.state = state
        entities[display.entityId] = key

        if (sameText && sameEntity) return
        display.trackedBy.forEach { player ->
            val cached = target.viewers[player.uniqueId]
            when {
                sameText && cached != null -> push(display.entityId, player, cached.component)
                state.text.external -> enqueue(Work(key, player.uniqueId))
                else -> refresh(target, state, player, forcePush = false)
            }
        }
    }

    override fun retain(npcId: String, lineCount: Int) {
        lines.entries.filter { (key, _) -> key.npcId == npcId.lowercase() && key.index >= lineCount }
            .forEach { (key, line) -> forget(key, line) }
    }

    override fun unbind(npcId: String) {
        lines.entries.filter { (key, _) -> key.npcId == npcId.lowercase() }
            .forEach { (key, line) -> forget(key, line) }
    }

    override fun unbindAll() {
        lines.clear()
        entities.clear()
        pending.clear()
    }

    override fun onPacketSend(event: PacketSendEvent) {
        if (event.packetType != PacketType.Play.Server.ENTITY_METADATA) return
        if (entities.isEmpty()) return

        val key = entities[peekEntityId(event.byteBuf)] ?: return
        val line = lines[key] ?: return
        val viewer = event.user.uuid ?: return
        val rendered = line.viewers[viewer]
        if (rendered == null) {
            requestRender(Work(key, viewer))
            return
        }

        val packet = WrapperPlayServerEntityMetadata(event)
        val rewrite = MetadataText.withText(
            packet.entityMetadata,
            textIndex,
            isText = { it.type == EntityDataTypes.ADV_COMPONENT },
            indexOf = EntityData<*>::getIndex,
            entry = { textEntry(it, rendered.component) },
        )
        textIndex = rewrite.textIndex
        packet.entityMetadata = rewrite.data
        event.markForReEncode(true)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTrack(event: PlayerTrackEntityEvent) {
        if (lines.isEmpty()) return
        val display = event.entity as? TextDisplay ?: return
        val key = entities[display.entityId] ?: adopt(display) ?: return
        val line = lines[key] ?: return

        renderNow(line, line.state, event.player)
    }

    @EventHandler
    fun onUntrack(event: PlayerUntrackEntityEvent) {
        if (event.entity !is TextDisplay) return
        val key = entities[event.entity.entityId] ?: return

        lines[key]?.viewers?.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val viewer = event.player.uniqueId
        lines.values.forEach { it.viewers.remove(viewer) }
        pending.removeIf { it.viewer == viewer }
    }

    @EventHandler
    fun onAddToWorld(event: EntityAddToWorldEvent) {
        if (lines.isEmpty()) return
        (event.entity as? TextDisplay)?.let(::adopt)
    }

    @EventHandler
    fun onRemoveFromWorld(event: EntityRemoveFromWorldEvent) {
        if (event.entity !is TextDisplay) return
        val id = event.entity.entityId
        val key = entities[id] ?: return

        entities.remove(id, key)
        lines[key]?.viewers?.clear()
    }

    private fun adopt(display: TextDisplay): HologramLineKey? {
        val container = display.persistentDataContainer
        val npcId = container.get(NpcKeys.HOLOGRAM, PersistentDataType.STRING) ?: return null
        val index = container.get(NpcKeys.HOLOGRAM_LINE, PersistentDataType.INTEGER) ?: return null
        val key = HologramLineKey.of(npcId, index)
        val line = lines[key] ?: return null
        val state = line.state
        if (state.entityUuid != display.uniqueId) return null

        if (state.entityId != display.entityId) {
            entities.remove(state.entityId, key)
            line.state = LineState(key, state.text, state.refreshTicks, state.entityUuid, display.entityId)
        }
        entities[display.entityId] = key

        return key
    }

    private fun forget(key: HologramLineKey, line: Line) {
        lines.remove(key, line)
        entities.remove(line.state.entityId, key)
    }

    private fun tick() {
        tick++
        if (lines.isEmpty()) return

        lines.values.forEach { line ->
            val state = line.state
            if (!state.text.external || (tick + state.phase) % state.refreshTicks != 0L) return@forEach
            val display = Bukkit.getEntity(state.entityUuid) as? TextDisplay ?: return@forEach
            val trackers = display.trackedBy
            line.viewers.keys.retainAll(trackers.mapTo(HashSet(), Player::getUniqueId))
            trackers.forEach { enqueue(Work(state.key, it.uniqueId)) }
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
            refresh(line, line.state, player, forcePush = false)
        } while (queue.isNotEmpty() && System.nanoTime() < deadline)

        if (queue.size > BACKLOG_WARNING) {
            CloudOutageLog.warnThrottled(
                "Hologram placeholders are falling behind (${queue.size} updates waiting). A PlaceholderAPI " +
                    "placeholder in a hologram line is slow; raise that line's refresh interval.",
                key = "hologram-backlog",
            )
        }
    }

    private fun refresh(line: Line, state: LineState, player: Player, forcePush: Boolean) {
        val ticket = tickets.incrementAndGet()
        val resolved = resolve(state, player)
        val current = line.viewers[player.uniqueId]
        if (current != null && current.source == state.text && current.resolved == resolved) {
            if (forcePush) push(state.entityId, player, current.component)
            return
        }

        if (worker.isShutdown) return
        val viewer = player.uniqueId
        worker.execute {
            val component = runCatching { componentFor(state, resolved) }
                .onFailure { NpcLog.logger.log(Level.WARNING, "Could not render a hologram line of '${state.key.npcId}'", it) }
                .getOrNull() ?: return@execute
            if (line.state !== state || !store(line, viewer, Rendered(ticket, state.text, resolved, component))) {
                return@execute
            }
            Bukkit.getPlayer(viewer)?.let { push(state.entityId, it, component) }
        }
    }

    private fun renderNow(line: Line, state: LineState, player: Player) {
        val ticket = tickets.incrementAndGet()
        val resolved = resolve(state, player)
        val current = line.viewers[player.uniqueId]
        if (current != null && current.source == state.text && current.resolved == resolved) return

        val component = runCatching { componentFor(state, resolved) }.getOrNull() ?: return
        store(line, player.uniqueId, Rendered(ticket, state.text, resolved, component))
    }

    private fun requestRender(work: Work) {
        if (!pending.add(work) || !plugin.isEnabled) return
        Bukkit.getScheduler().runTask(plugin, Runnable {
            pending.remove(work)
            val line = lines[work.key] ?: return@Runnable
            val player = Bukkit.getPlayer(work.viewer) ?: return@Runnable
            refresh(line, line.state, player, forcePush = true)
        })
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

    private fun push(entityId: Int, player: Player, component: PacketComponent) {
        val packet = WrapperPlayServerEntityMetadata(entityId, listOf(textEntry(textIndex, component)))
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private fun textEntry(index: Int, text: PacketComponent): EntityData<*> =
        EntityData(index, EntityDataTypes.ADV_COMPONENT, text)

    private fun peekEntityId(buffer: Any): Int {
        val start = ByteBufHelper.readerIndex(buffer)

        return try {
            ByteBufHelper.readVarInt(buffer)
        } finally {
            ByteBufHelper.readerIndex(buffer, start)
        }
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
