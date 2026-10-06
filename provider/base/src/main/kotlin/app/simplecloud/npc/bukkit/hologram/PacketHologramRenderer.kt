package app.simplecloud.npc.bukkit.hologram

import app.simplecloud.npc.bukkit.compat.EntityIds
import app.simplecloud.npc.bukkit.packet.PacketComponents
import app.simplecloud.npc.bukkit.packet.PacketTextDisplays
import app.simplecloud.npc.bukkit.packet.PacketViewerTracker
import app.simplecloud.npc.bukkit.scheduling.sync
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.hologram.HologramLineResolver
import app.simplecloud.npc.core.hologram.HologramLineResolver.ResolvedLine
import app.simplecloud.npc.core.render.HologramRenderer
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import app.simplecloud.npc.relocate.packets.kyori.adventure.text.Component as PacketComponent

class PacketHologramRenderer(
    private val plugin: Plugin,
    private val tracker: PacketViewerTracker,
) : HologramRenderer {

    private data class Line(
        val key: HologramLineKey,
        val entityId: Int,
        val uuid: UUID,
        val y: Double,
        val style: HologramLine,
        val shared: PacketComponent,
    )

    private val refreshScope = RefreshScope("hologram")
    private val holograms = ConcurrentHashMap<String, Hologram>()

    val texts = ViewerTexts(plugin, ::viewersOf, ::pushText)

    override fun createOrUpdate(config: NpcConfig) {
        if (!config.hologram.enabled) {
            destroy(config)
            return
        }
        refreshScope.launch(config.id) { rebuild(config) }
    }

    override fun destroy(config: NpcConfig) {
        refreshScope.launch(config.id) { plugin.sync { remove(config.id) } }
    }

    override fun destroyAll() {
        refreshScope.cancel()
        plugin.sync { holograms.keys.toList().forEach(::remove) }
    }

    private suspend fun rebuild(config: NpcConfig) {
        val lines = when (val resolved = HologramLineResolver.resolve(config)) {
            is HologramLineResolver.Result.Lines -> resolved.lines
            HologramLineResolver.Result.Unavailable -> return
            HologramLineResolver.Result.None -> {
                plugin.sync { remove(config.id) }
                return
            }
        }

        plugin.sync { apply(config, lines) }
    }

    private fun apply(config: NpcConfig, resolved: List<ResolvedLine>) {
        val id = config.id.lowercase(Locale.ROOT)
        val location = config.entity.location
        val world = Bukkit.getWorld(location.world) ?: return remove(id)
        val heights = HologramGeometry.lineHeights(config, resolved.size)
        val existing = holograms[id]

        resolved.forEachIndexed { index, line ->
            val personal = line.personal?.let { PersonalLine(it, line.line.refreshSeconds) }
            texts.track(HologramLineKey.of(id, index), personal)
        }
        texts.retain(id, resolved.size)

        val lines = resolved.mapIndexed { index, line ->
            val previous = existing?.lines?.getOrNull(index)
            Line(
                key = HologramLineKey.of(id, index),
                entityId = previous?.entityId ?: EntityIds.next(world),
                uuid = previous?.uuid ?: UUID.randomUUID(),
                y = location.y + heights[index],
                style = line.line,
                shared = PacketComponents.of(line.text),
            )
        }

        if (existing == null) {
            val hologram = Hologram(id, location.world, location.x, location.y, location.z, config.entity.viewDistance)
            hologram.lines = lines
            holograms[id] = hologram
            tracker.add(hologram)
            return
        }

        existing.range = config.entity.viewDistance
        val moved = existing.world != location.world || existing.x != location.x || existing.z != location.z
        val reshaped = moved || existing.lines.size != lines.size ||
            existing.lines.zip(lines).any { (old, new) -> old.y != new.y }
        val viewers = existing.viewers.mapNotNull(Bukkit::getPlayer)

        if (reshaped) {
            val stale = existing.lines.map(Line::entityId)
            viewers.forEach { PacketTextDisplays.send(it, listOf(PacketTextDisplays.destroy(stale))) }
            existing.lines = lines.map { it.copy(entityId = EntityIds.next(world), uuid = UUID.randomUUID()) }
            existing.moveTo(location.world, location.x, location.y, location.z)
            viewers.forEach { existing.show(it) }
            return
        }

        val previous = existing.lines
        existing.lines = lines
        viewers.forEach { player ->
            val updates = lines.zip(previous).mapNotNull { (line, old) -> update(line, old, player) }
            PacketTextDisplays.send(player, updates)
        }
    }

    private fun update(line: Line, old: Line, player: Player): PacketWrapper<*>? {
        val personal = texts.isPersonal(line.key)
        if (line.style != old.style) {
            return PacketTextDisplays.metadata(line.entityId, line.style, textFor(line, player))
        }
        if (!personal && line.shared != old.shared) return PacketTextDisplays.text(line.entityId, line.shared)

        return null
    }

    private fun textFor(line: Line, player: Player): PacketComponent = texts.textFor(line.key, player) ?: line.shared

    private fun remove(npcId: String) {
        val id = npcId.lowercase(Locale.ROOT)
        val hologram = holograms.remove(id)
        texts.forget(id)
        hologram ?: return

        val ids = hologram.lines.map(Line::entityId)
        tracker.remove(hologram).forEach { PacketTextDisplays.send(it, listOf(PacketTextDisplays.destroy(ids))) }
    }

    private fun viewersOf(key: HologramLineKey): Collection<Player> =
        holograms[key.npcId]?.viewers?.mapNotNull(Bukkit::getPlayer).orEmpty()

    private fun pushText(key: HologramLineKey, player: Player, text: PacketComponent) {
        val hologram = holograms[key.npcId] ?: return
        if (player.uniqueId !in hologram.viewers) return
        val line = hologram.lines.getOrNull(key.index) ?: return

        PacketTextDisplays.send(player, listOf(PacketTextDisplays.text(line.entityId, text)))
    }

    private inner class Hologram(
        private val npcId: String,
        world: String,
        x: Double,
        y: Double,
        z: Double,
        range: Double,
    ) : PacketViewerTracker.Entry("Hologram $npcId", world, x, y, z, range) {
        @Volatile
        var lines: List<Line> = emptyList()

        override fun show(player: Player) {
            val packets = lines.flatMap { line ->
                listOf(
                    PacketTextDisplays.spawn(line.entityId, line.uuid, x, line.y, z),
                    PacketTextDisplays.metadata(line.entityId, line.style, textFor(line, player)),
                )
            }
            PacketTextDisplays.send(player, packets)
        }

        override fun hide(player: Player) {
            PacketTextDisplays.send(player, listOf(PacketTextDisplays.destroy(lines.map(Line::entityId))))
            texts.viewerGone(npcId, player.uniqueId)
        }

        override fun forget(uuid: UUID) = texts.viewerGone(npcId, uuid)
    }
}
