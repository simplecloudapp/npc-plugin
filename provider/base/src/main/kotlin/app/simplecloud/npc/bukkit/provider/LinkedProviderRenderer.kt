package app.simplecloud.npc.bukkit.provider

import app.simplecloud.npc.bukkit.location.requireLoadedWorld
import app.simplecloud.npc.bukkit.scheduling.sync
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.core.render.NpcRenderer
import app.simplecloud.npc.core.render.ProviderNpcSnapshot
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

abstract class LinkedProviderRenderer<T : Any>(
    protected val plugin: Plugin,
    private val providerLabel: String,
    private val worldLookup: (name: String) -> World? = Bukkit::getWorld,
    private val sync: (block: () -> Unit) -> Unit = { block -> plugin.sync(block) },
) : NpcRenderer {

    override val supportsLinking: Boolean = true

    private val trackedByConfigId = ConcurrentHashMap<String, T>()
    private val configIdByEventKey = ConcurrentHashMap<String, String>()
    private val linkedConfigIds = ConcurrentHashMap.newKeySet<String>()
    private val listeners = mutableListOf<Listener>()

    protected val logger: Logger = NpcLog.logger

    protected abstract fun eventKeyOf(tracked: T): String
    protected abstract fun findByReference(reference: String): T?
    protected abstract fun createListener(): Listener

    protected abstract fun linkCandidates(): List<Pair<String, T>>

    protected open fun create(config: NpcConfig, world: World): T? = null
    protected open fun destroy(tracked: T) = Unit
    protected open fun deleteLegacy(legacy: T) = Unit

    protected open fun createReadinessListener(): Listener? = null
    protected open fun refreshOwned(config: NpcConfig, tracked: T) = Unit
    protected open fun teleportOwned(config: NpcConfig, tracked: T, world: World) = Unit

    protected abstract fun locationOf(tracked: T): NpcLocation?

    protected open fun snapshotOf(found: T, config: NpcConfig): ProviderNpcSnapshot? =
        ProviderNpcSnapshot(locationOf(found))

    protected open fun onEnabled() = Unit
    protected open fun onDisabling() = Unit
    protected open fun onDisabled() = Unit

    fun configIdForEventKey(key: String): String? = configIdByEventKey[key]

    protected fun tracked(configId: String): T? = trackedByConfigId[configId]
    protected fun isLinked(configId: String): Boolean = configId in linkedConfigIds
    protected fun worldOf(config: NpcConfig): World? = worldLookup(config.entity.location.world)

    final override fun linkableReferences(): List<String> {
        val owned = ownedEventKeys()
        return linkCandidates()
            .filter { (_, candidate) -> eventKeyOf(candidate) !in owned }
            .map { it.first }
            .distinct()
    }

    private fun ownedEventKeys(): Set<String> =
        trackedByConfigId.filterKeys { it !in linkedConfigIds }.values.mapTo(hashSetOf(), ::eventKeyOf)

    final override fun onEnable() {
        listeners += listOfNotNull(createListener(), createReadinessListener())
        listeners.forEach { Bukkit.getPluginManager().registerEvents(it, plugin) }
        onEnabled()
    }

    final override fun onDisable() {
        listeners.forEach(HandlerList::unregisterAll)
        listeners.clear()
        onDisabling()

        trackedByConfigId.forEach { (configId, tracked) ->
            if (configId !in linkedConfigIds) runCatching { destroy(tracked) }
        }
        trackedByConfigId.clear()
        configIdByEventKey.clear()
        linkedConfigIds.clear()
        onDisabled()
    }

    final override fun spawn(config: NpcConfig): NpcConfig {
        sync { spawnNow(config) }
        return config
    }

    private fun spawnNow(config: NpcConfig) {
        untrack(config.id)
        if (config.entity.providerLinked || !supportsCreation) {
            attach(config)
            return
        }

        val world = requireLoadedWorld(providerLabel, config, logger, worldLookup) ?: return
        val created = create(config, world) ?: return
        track(config.id, created)
    }

    final override fun despawn(config: NpcConfig) = sync { despawnNow(config) }

    private fun despawnNow(config: NpcConfig) {
        if (trackedByConfigId.containsKey(config.id)) {
            untrack(config.id)
            return
        }
        if (config.entity.providerLinked || !supportsCreation) return

        val reference = config.entity.providerReference ?: return
        val legacy = findByReference(reference) ?: return

        logger.info("Deleting legacy persisted $providerLabel NPC '$reference' (NPC '${config.id}').")
        deleteLegacy(legacy)
    }

    final override fun refresh(config: NpcConfig) {
        if (isLinked(config.id)) return
        val tracked = tracked(config.id) ?: return
        sync { refreshOwned(config, tracked) }
    }

    final override fun teleport(config: NpcConfig) {
        if (isLinked(config.id)) return
        val tracked = tracked(config.id) ?: return
        sync {
            val world = worldOf(config) ?: return@sync
            teleportOwned(config, tracked, world)
        }
    }

    final override fun locationOf(config: NpcConfig): NpcLocation? = tracked(config.id)?.let(::locationOf)

    final override fun snapshotOf(config: NpcConfig): ProviderNpcSnapshot? {
        var result: ProviderNpcSnapshot? = null
        sync {
            val reference = config.entity.providerReference ?: return@sync
            val found = findByReference(reference) ?: return@sync
            result = snapshotOf(found, config)
        }

        return result
    }

    final override fun canonicalReference(config: NpcConfig): String? =
        if (isLinked(config.id)) tracked(config.id)?.let(::referenceOf) else null

    protected open fun referenceOf(tracked: T): String? = null

    private fun attach(config: NpcConfig) {
        val reference = config.entity.providerReference ?: run {
            logger.warning(
                if (supportsCreation) {
                    "NPC '${config.id}' is marked as linked but has no $providerLabel reference; " +
                        "the link stays inactive."
                } else {
                    "NPC '${config.id}' uses provider $providerLabel, which can only link existing NPCs, " +
                        "but has no reference; skipping."
                },
            )
            return
        }

        val existing = findByReference(reference) ?: run {
            logger.warning(
                "NPC '${config.id}' is linked to $providerLabel NPC '$reference', which doesn't exist or " +
                    "isn't loaded; the link stays inactive.",
            )
            return
        }

        if (eventKeyOf(existing) in ownedEventKeys()) {
            logger.warning(
                "NPC '${config.id}' is linked to $providerLabel NPC '$reference', which is one this plugin " +
                    "created for another config; the link stays inactive.",
            )
            return
        }

        track(config.id, existing)
        linkedConfigIds += config.id
    }

    private fun track(configId: String, tracked: T) {
        trackedByConfigId[configId] = tracked
        configIdByEventKey[eventKeyOf(tracked)] = configId
    }

    protected fun untrack(configId: String) {
        val tracked = trackedByConfigId.remove(configId) ?: return
        configIdByEventKey.remove(eventKeyOf(tracked))
        if (!linkedConfigIds.remove(configId)) destroy(tracked)
    }
}
