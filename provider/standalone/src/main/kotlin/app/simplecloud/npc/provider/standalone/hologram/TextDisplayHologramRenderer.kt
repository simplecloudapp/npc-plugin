package app.simplecloud.npc.provider.standalone.hologram

import app.simplecloud.npc.bukkit.NpcKeys
import app.simplecloud.npc.bukkit.hologram.HologramLineKey
import app.simplecloud.npc.bukkit.hologram.PersonalLine
import app.simplecloud.npc.bukkit.hologram.RefreshScope
import app.simplecloud.npc.bukkit.hologram.ViewerHologramText
import app.simplecloud.npc.bukkit.location.toBukkitLocation
import app.simplecloud.npc.bukkit.provider.WorldReconciler
import app.simplecloud.npc.bukkit.scheduling.sync
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.hologram.HologramLineResolver
import app.simplecloud.npc.core.hologram.HologramLineResolver.ResolvedLine
import app.simplecloud.npc.core.render.HologramRenderer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.floor

class TextDisplayHologramRenderer(
    private val plugin: Plugin,
) : HologramRenderer, WorldReconciler, Listener {

    private val refreshScope = RefreshScope("hologram")
    private val textDisplays = ConcurrentHashMap<UUID, String>()

    override var knownIdsProvider: (() -> Set<String>)? = null

    @Volatile
    var viewerText: ViewerHologramText = ViewerHologramText.NONE

    fun start() {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    override fun createOrUpdate(config: NpcConfig) {
        if (!config.hologram.enabled) {
            destroy(config)
            return
        }
        refreshScope.launch(config.id) { rebuild(config) }
    }

    override fun destroy(config: NpcConfig) {
        refreshScope.launch(config.id) { plugin.sync { destroyLocked(config) } }
    }

    override fun destroyAll() {
        refreshScope.cancel()
        HandlerList.unregisterAll(this)
        plugin.sync {
            viewerText.unbindAll()
            textDisplays.keys.mapNotNull(Bukkit::getEntity).filterIsInstance<TextDisplay>().forEach(TextDisplay::remove)
            textDisplays.clear()
        }
    }

    override fun reconcileLoadedWorlds() {
        Bukkit.getWorlds().forEach(::seedFromWorld)
        destroyOrphans()
    }

    @EventHandler
    fun onWorldLoad(event: WorldLoadEvent) {
        seedFromWorld(event.world)
        destroyOrphans()
    }

    @EventHandler
    fun onEntitiesLoad(event: EntitiesLoadEvent) {
        val tagged = event.entities.filterIsInstance<TextDisplay>().mapNotNull { display ->
            display.persistentDataContainer.get(NpcKeys.HOLOGRAM, PersistentDataType.STRING)
                ?.also { id -> textDisplays[display.uniqueId] = id }
        }

        if (tagged.isNotEmpty()) destroyOrphans()
    }

    private suspend fun rebuild(config: NpcConfig) {
        val lines = when (val resolved = HologramLineResolver.resolve(config)) {
            is HologramLineResolver.Result.Lines -> resolved.lines
            HologramLineResolver.Result.Unavailable -> return
            HologramLineResolver.Result.None -> {
                plugin.sync { destroyLocked(config) }
                return
            }
        }

        plugin.sync {
            val location = config.entity.location
            val world = Bukkit.getWorld(location.world) ?: return@sync
            if (!entitiesLoaded(world, location.x, location.z)) return@sync

            val current = existing(config, world)
            if (current != null && sameSpot(current, config, lines.size)) {
                patchTexts(config.id, current, lines)
                return@sync
            }

            replace(config, world, lines)
        }
    }

    private fun seedFromWorld(world: World) {
        world.entities.filterIsInstance<TextDisplay>().forEach { display ->
            display.persistentDataContainer.get(NpcKeys.HOLOGRAM, PersistentDataType.STRING)?.let { id ->
                textDisplays[display.uniqueId] = id
            }
        }
    }

    private fun destroyOrphans() {
        val knownIds = knownIdsProvider?.invoke() ?: return
        val orphaned = textDisplays.filterValues { it.lowercase() !in knownIds }
        if (orphaned.isEmpty()) return

        plugin.sync {
            orphaned.values.distinct().forEach(viewerText::unbind)
            orphaned.keys.mapNotNull(Bukkit::getEntity).filterIsInstance<TextDisplay>().forEach(TextDisplay::remove)
            orphaned.keys.forEach(textDisplays::remove)
        }

        orphaned.values.distinct().forEach {
            logger.info("Removed a leftover hologram of NPC '$it': the NPC is gone or no longer shows one here.")
        }
    }

    private fun existing(config: NpcConfig, world: World): List<TextDisplay>? {
        val location = config.entity.location
        reconcileChunk(config.id, world, location.x, location.z)

        return displaysOf(config.id).sortedBy {
            it.persistentDataContainer.get(NpcKeys.HOLOGRAM_LINE, PersistentDataType.INTEGER) ?: 0
        }.takeIf { it.isNotEmpty() }
    }

    private fun sameSpot(existing: List<TextDisplay>, config: NpcConfig, lineCount: Int): Boolean {
        if (existing.size != lineCount) return false
        val bottom = existing.first().location
        val location = config.entity.location

        return bottom.world?.name == location.world &&
            near(bottom.x, location.x) &&
            near(bottom.y, location.y + config.hologram.startHeight * config.entity.heightFactor()) &&
            near(bottom.z, location.z)
    }

    private fun patchTexts(id: String, existing: List<TextDisplay>, lines: List<ResolvedLine>) {
        lines.reversed().forEachIndexed { index, resolved ->
            val display = existing.getOrNull(index) ?: return@forEachIndexed
            bind(id, index, display, resolved)
            if (display.text() != resolved.text) display.text(resolved.text)
            apply(resolved.line, display)
        }
        viewerText.retain(id, lines.size)
    }

    private fun bind(id: String, index: Int, display: TextDisplay, resolved: ResolvedLine) {
        val personal = resolved.personal?.let { PersonalLine(it, resolved.line.refreshSeconds) }
        viewerText.bind(display, HologramLineKey.of(id, index), personal, resolved.text)
    }

    private fun replace(config: NpcConfig, world: World, lines: List<ResolvedLine>) {
        val id = config.id
        val npcLocation = config.entity.location.toBukkitLocation(world)
        val startHeight = config.hologram.startHeight * config.entity.heightFactor()

        val created = mutableListOf<TextDisplay>()
        try {
            lines.reversed().forEachIndexed { index, resolved ->
                val lineLocation = npcLocation.clone().add(0.0, startHeight + LINE_SPACING * index, 0.0)
                created += spawn(id, lineLocation, index, resolved)
            }
        } catch (exception: Exception) {
            created.forEach(TextDisplay::remove)
            throw exception
        }

        removeStale(id, created.mapTo(hashSetOf(), TextDisplay::getUniqueId))
        created.forEach { textDisplays[it.uniqueId] = id }
        viewerText.retain(id, lines.size)
    }

    private fun destroyLocked(config: NpcConfig) {
        val id = config.id
        val location = config.entity.location
        Bukkit.getWorld(location.world)?.let { world -> reconcileChunk(id, world, location.x, location.z) }

        viewerText.unbind(id)
        displaysOf(id).forEach(TextDisplay::remove)
        textDisplays.entries.removeIf { it.value == id }
    }

    private fun spawn(id: String, location: Location, lineIndex: Int, resolved: ResolvedLine): TextDisplay {
        val world = location.world ?: error("Cannot spawn hologram '$id': its world is not loaded.")

        return world.spawn(location, TextDisplay::class.java) { display ->
            display.billboard = Display.Billboard.CENTER
            display.text(resolved.text)
            display.persistentDataContainer.set(NpcKeys.HOLOGRAM, PersistentDataType.STRING, id)
            display.persistentDataContainer.set(NpcKeys.HOLOGRAM_LINE, PersistentDataType.INTEGER, lineIndex)
            apply(resolved.line, display)
            bind(id, lineIndex, display, resolved)
        }
    }

    private fun apply(configuration: HologramLine, textDisplay: TextDisplay) {
        textDisplay.apply {
            billboard = Display.Billboard.valueOf(configuration.billboard.name)
            alignment = TextDisplay.TextAlignment.valueOf(configuration.alignment.name)
            configuration.viewRange?.let { viewRange = it }
            configuration.shadowRadius?.let { shadowRadius = it }
            configuration.displayHeight?.let { displayHeight = it }
            configuration.displayWidth?.let { displayWidth = it }
            configuration.shadow?.let { isShadowed = it }
            configuration.lineWidth?.let { lineWidth = it }
        }
    }

    private fun removeStale(id: String, keep: Set<UUID>) {
        displaysOf(id).filterNot { it.uniqueId in keep }.forEach(TextDisplay::remove)
        textDisplays.entries.removeIf { it.value == id && it.key !in keep }
    }

    private fun displaysOf(id: String): List<TextDisplay> =
        textDisplays.filterValues { it == id }.keys.mapNotNull(Bukkit::getEntity).filterIsInstance<TextDisplay>()

    private fun reconcileChunk(id: String, world: World, x: Double, z: Double) {
        if (textDisplays.containsValue(id)) return
        if (!entitiesLoaded(world, x, z)) return

        world.getChunkAt(chunkIndex(x), chunkIndex(z)).entities.filterIsInstance<TextDisplay>().forEach { display ->
            display.persistentDataContainer.get(NpcKeys.HOLOGRAM, PersistentDataType.STRING)?.let { tagId ->
                textDisplays[display.uniqueId] = tagId
            }
        }
    }

    private fun entitiesLoaded(world: World, x: Double, z: Double): Boolean {
        val chunkX = chunkIndex(x)
        val chunkZ = chunkIndex(z)

        return world.isChunkLoaded(chunkX, chunkZ) && world.getChunkAt(chunkX, chunkZ).isEntitiesLoaded
    }

    private fun chunkIndex(coordinate: Double): Int = floor(coordinate).toInt() shr 4

    private fun near(a: Double, b: Double): Boolean = abs(a - b) < LOCATION_EPSILON

    private companion object {
        private const val LINE_SPACING = 0.3
        private const val LOCATION_EPSILON = 0.001
        private val logger = NpcLog.logger
    }
}
