package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.bridge.ServerBridge
import app.simplecloud.npc.shared.bridge.ServerBridgeFinder
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.createAtNamespacedKey
import app.simplecloud.npc.shared.hologram.config.HologramConfiguration
import app.simplecloud.npc.shared.hologramLineNamespacedKey
import app.simplecloud.npc.shared.hologramNamespacedKey
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.sync
import app.simplecloud.npc.shared.utils.NpcFileUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class HologramManager(
    private val namespace: NpcNamespace,
) {
    private val textDisplays = hashMapOf<UUID, String>()
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshJobs = ConcurrentHashMap<String, Job>()
    private val refreshLocks = ConcurrentHashMap<String, Mutex>()
    private val generations = ConcurrentHashMap<String, AtomicLong>()

    fun registerFileRequest() {
        NpcFileUpdater.updateFileRequest("hologram", ::createOrUpdate)
    }

    fun createOrUpdate(config: NpcConfig) {
        if (!config.hologram.enabled) {
            destroyHolograms(config.id)
            return
        }

        refreshJobs.remove(config.id)?.cancel()
        refreshJobs[config.id] = refreshScope.launchRefresh(config.id)
    }

    suspend fun updateHolograms(config: NpcConfig): Boolean {
        return refreshLock(config.id).withLock { rebuildHologram(config) }
    }

    private suspend fun rebuildHologram(config: NpcConfig): Boolean {
        if (!config.hologram.enabled) {
            removeHolograms(config.id)
            return true
        }
        val layout = config.hologram.getLayout(JoinStateHelper.getJoinState(config))
        if (layout == null) {
            removeHolograms(config.id)
            return true
        }
        val generation = generation(config.id)
        val location = sync { namespace.findLocationByNpc(config.id)?.clone() } ?: return false
        val serverBridge = getServerBridge(config)
        val renderedLines = layout.lines.map {
            it to HologramPlaceholderHelper.appendPlaceholder(serverBridge, it.text)
        }
        return sync {
            if (generation != generation(config.id)) return@sync false
            replaceHologram(config.id, location, config.hologram.startHeight, renderedLines)
            true
        }
    }

    private fun replaceHologram(
        id: String,
        npcLocation: Location,
        startHeight: Double,
        lines: List<Pair<HologramConfiguration, Component>>,
    ) {
        val created = mutableListOf<TextDisplay>()
        try {
            lines.reversed().forEachIndexed { index, (line, component) ->
                val location = npcLocation.clone().add(0.0, startHeight + LINE_SPACING * index, 0.0)
                val editor = HologramEditor(id, location, index).withCustomName(component)
                HologramModifier.modify(line, editor.textDisplay)
                created += editor.textDisplay
            }
        } catch (exception: Exception) {
            created.forEach(TextDisplay::remove)
            throw exception
        }

        removeHolograms(id, created.mapTo(hashSetOf(), TextDisplay::getUniqueId))
        created.forEach { textDisplays[it.uniqueId] = id }
    }

    suspend fun updateTextHologram(config: NpcConfig, state: String? = null) {
        refreshLock(config.id).withLock { updateTextHologramLocked(config, state) }
    }

    private suspend fun updateTextHologramLocked(config: NpcConfig, state: String?) {
        if (!config.hologram.enabled) {
            removeHolograms(config.id)
            return
        }
        val layout = config.hologram.getLayout(state ?: JoinStateHelper.getJoinState(config))
        if (layout == null) {
            removeHolograms(config.id)
            return
        }
        val displays = sync {
            getTextDisplays(config.id).sortedWith(textDisplayComparator)
        }
        if (displays.size != layout.lines.size) {
            rebuildHologram(config)
            return
        }
        val serverBridge = getServerBridge(config)
        layout.lines.reversed().forEachIndexed { index, line ->
            val component = HologramPlaceholderHelper.appendPlaceholder(serverBridge, line.text)
            sync { displays.getOrNull(index)?.text(component) }
        }
    }

    fun destroyHolograms(id: String) {
        refreshJobs.remove(id)?.cancel()
        generations.computeIfAbsent(id) { AtomicLong() }.incrementAndGet()
        removeHolograms(id)
    }

    private fun removeHolograms(id: String, excluding: Set<UUID> = emptySet()) {
        sync {
            getTextDisplays(id).filterNot { it.uniqueId in excluding }.forEach(TextDisplay::remove)
            textDisplays.entries.removeIf { it.value == id && it.key !in excluding }

            val location = namespace.findLocationByNpc(id) ?: return@sync
            location.world.entities
                .filterIsInstance<TextDisplay>()
                .filter { it.uniqueId !in excluding }
                .filter { it.persistentDataContainer.get(hologramNamespacedKey, PersistentDataType.STRING) == id }
                .forEach(TextDisplay::remove)
        }
    }

    fun destroyLegacyHolograms(id: String) {
        sync {
            val location = namespace.findLocationByNpc(id) ?: return@sync
            location.world.entities
                .filter { it.persistentDataContainer.get(hologramNamespacedKey, PersistentDataType.STRING) == id }
                .forEach { it.remove() }
        }
    }

    fun destroyAllHolograms() {
        refreshJobs.values.forEach(Job::cancel)
        refreshJobs.clear()
        refreshScope.cancel()
        sync {
            namespace.findAllNpcs()
                .mapNotNull(namespace::findLocationByNpc)
                .map { it.world }
                .distinct()
                .flatMap { it.entities }
                .filterIsInstance<TextDisplay>()
                .filter { it.persistentDataContainer.has(hologramNamespacedKey) }
                .forEach { it.remove() }
            textDisplays.clear()
        }
    }

    fun getTextDisplays(id: String): List<TextDisplay> {
        return sync {
            textDisplays.filterValues { it == id }
                .keys
                .mapNotNull(Bukkit::getEntity)
                .filterIsInstance<TextDisplay>()
        }
    }

    private fun CoroutineScope.launchRefresh(id: String): Job = launch {
        var lastFailure: Exception? = null
        repeat(CREATE_ATTEMPTS) { attempt ->
            val config = namespace.npcRepository.find(id) ?: return@launch
            try {
                if (updateHolograms(config)) return@launch
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                lastFailure = exception
            }

            if (attempt < CREATE_ATTEMPTS - 1) delay(CREATE_RETRY_MILLIS)
        }

        val reason = lastFailure?.message ?: "provider NPC location is not available"
        Bukkit.getLogger().warning("[SimpleCloud-NPC] Could not create hologram $id: $reason")
    }

    private fun refreshLock(id: String): Mutex = refreshLocks.computeIfAbsent(id) { Mutex() }

    private fun generation(id: String): Long = generations.computeIfAbsent(id) { AtomicLong() }.get()

    private suspend fun getServerBridge(config: NpcConfig): ServerBridge {
        val target = requireNotNull(config.targetServers.firstOrNull()) { "NPC ${config.id} has no target server" }
        return requireNotNull(ServerBridgeFinder.find(target)) { "Failed to find target $target for NPC ${config.id}" }
    }

    companion object {
        private const val LINE_SPACING = 0.3
        private const val CREATE_ATTEMPTS = 6
        private const val CREATE_RETRY_MILLIS = 1_000L

        private val textDisplayComparator = compareBy<TextDisplay> {
            it.persistentDataContainer.get(hologramLineNamespacedKey, PersistentDataType.INTEGER) ?: Int.MAX_VALUE
        }.thenBy {
            it.persistentDataContainer.get(createAtNamespacedKey, PersistentDataType.LONG) ?: Long.MAX_VALUE
        }.thenBy { it.uniqueId }
    }
}
