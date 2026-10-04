package app.simplecloud.npc.core.repository

import app.simplecloud.npc.core.NpcLog
import app.simplecloud.plugin.api.shared.config.ConfigMigrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.spongepowered.configurate.ConfigurationNode
import java.io.File
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.zip.CRC32
import kotlin.time.Duration.Companion.milliseconds

class ReloadDiff<E>(
    val added: List<E> = emptyList(),
    val changed: List<Change<E>> = emptyList(),
    val removed: List<E> = emptyList(),
) {
    class Change<E>(val previous: E, val current: E)
}

abstract class WatchableYamlDirectoryRepository<E : Any>(
    private val directory: Path,
    clazz: Class<E>,
    migrator: ConfigMigrator? = null,
    prepare: (ConfigurationNode) -> Unit = {},
) {
    private val configurator = YamlConfigurator(clazz, migrator, prepare)
    private val cachedEntities = ConcurrentHashMap<String, E>()
    private val byId = ConcurrentHashMap<String, E>()
    private val changeListeners = ConcurrentHashMap.newKeySet<() -> Unit>()
    private val recentOwnDeletes = ConcurrentHashMap<String, Long>()
    private val ownContentHashes = ConcurrentHashMap<String, Long>()
    private val pendingLoads = ConcurrentHashMap<String, Job>()

    @Volatile
    private var externalChangeListener: (ReloadDiff<E>) -> Unit = {}

    @Volatile
    private var watchScope: CoroutineScope? = null

    @Volatile
    private var watchJob: Job? = null

    @Volatile
    private var watchService: WatchService? = null

    protected abstract fun idOf(entity: E): String
    protected abstract fun detach(entity: E): E

    protected open fun beforeSave(entity: E) = Unit

    fun save(entity: E) {
        val detached = detach(entity).also(::beforeSave)
        save(fileNameOf(idOf(detached)) ?: fileNameFor(detached), detached)
    }

    fun find(id: String): E? = byId[id.lowercase()]?.let(::detach)
    fun findAll(): List<E> = cachedEntities.values.map(::detach)
    fun ids(): List<String> = byId.values.map(::idOf).sortedWith(String.CASE_INSENSITIVE_ORDER)

    fun delete(id: String) {
        val fileName = fileNameOf(id) ?: return

        recentOwnDeletes[fileName] = System.currentTimeMillis()
        val file = directory.resolve(fileName).toFile()
        if (!file.delete() && file.exists()) {
            logger.warning("Could not delete ${file.name}; it will come back the next time the directory is read")
        }

        removeEntity(fileName)
    }

    fun load(): List<E> {
        val directoryFile = directory.toFile()
        directoryFile.mkdirs()
        directoryFile.listFiles { file -> file.isYamlFile() }?.forEach { load(it) }

        return findAll()
    }

    private fun fileNameFor(entity: E): String = "${idOf(entity)}.yml"

    private fun fileNameOf(id: String): String? {
        val entity = byId[id.lowercase()] ?: return null
        return cachedEntities.entries.firstOrNull { it.value === entity }?.key
    }

    private fun load(file: File): E? {
        val entity = runCatching { configurator.load(file) }
            .onFailure { logger.log(Level.WARNING, "Could not load ${file.name}", it) }
            .getOrNull() ?: return null

        val expected = fileNameFor(entity)
        if (!expected.equals(file.name, ignoreCase = true)) {
            logger.warning(
                "Skipping ${file.name}: it holds '$expected', so it would show up twice. " +
                    "Rename the file to $expected or change the id inside it.",
            )
            return null
        }

        ownContentHashes[file.name] = contentHash(file)
        putEntity(file.name, entity)

        return entity
    }

    fun reload(): ReloadDiff<E> {
        val before = cachedEntities.toMap()
        val onDisk = directory.toFile().listFiles { file -> file.isYamlFile() }?.map { it.name }.orEmpty().toSet()

        val removed = before.filterKeys { it !in onDisk }.onEach { (fileName, _) -> removeEntity(fileName) }.values
        load()
        val after = cachedEntities.toMap()

        val added = after.filterKeys { it !in before }.values.map(::detach)
        val changed = after.mapNotNull { (fileName, current) ->
            before[fileName]?.takeIf { it != current }?.let { ReloadDiff.Change(detach(it), detach(current)) }
        }

        return ReloadDiff(added, changed, removed.map(::detach))
    }

    private fun save(fileName: String, entity: E) {
        val file = directory.resolve(fileName).toFile()
        file.parentFile?.mkdirs()
        configurator.save(file, entity)
        ownContentHashes[fileName] = contentHash(file)
        putEntity(fileName, entity)
    }

    fun onAnyChange(listener: () -> Unit) {
        changeListeners.add(listener)
    }

    fun setExternalChangeListener(listener: (ReloadDiff<E>) -> Unit) {
        externalChangeListener = listener
    }

    fun loadAndWatch(): List<E> {
        val entities = load()
        watchUpdates()

        return entities
    }

    private fun watchUpdates() {
        if (watchJob?.isActive == true) return

        directory.toFile().mkdirs()
        val service = FileSystems.getDefault().newWatchService()
        try {
            directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        } catch (exception: Exception) {
            service.close()
            throw exception
        }
        watchService = service
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { watchScope = it }

        watchJob = scope.launch {
            while (isActive) {
                val watchKey = try {
                    service.take()
                } catch (_: ClosedWatchServiceException) {
                    break
                } catch (_: InterruptedException) {
                    break
                }
                watchKey.pollEvents().forEach { event ->
                    try {
                        val path = event.context() as? Path ?: return@forEach
                        val file = directory.resolve(path).toFile()
                        if (!file.isYamlFile()) return@forEach

                        when (event.kind()) {
                            ENTRY_CREATE, ENTRY_MODIFY -> scheduleLoad(scope, file)
                            ENTRY_DELETE -> if (!isOwnDeleteEcho(file.name)) {
                                pendingLoads.remove(file.name)?.cancel()
                                cachedEntities[file.name]?.let { entity ->
                                    removeEntity(file.name)
                                    externalChangeListener(ReloadDiff(removed = listOf(detach(entity))))
                                }
                            }
                        }
                    } catch (exception: Exception) {
                        logger.log(Level.WARNING, "Failed to handle a config directory change in $directory", exception)
                    }
                }
                if (!watchKey.reset() && !watchAgain(service)) break
            }
        }
    }

    private suspend fun CoroutineScope.watchAgain(service: WatchService): Boolean {
        while (isActive) {
            delay(REWATCH_DELAY_MILLIS.milliseconds)
            if (!directory.toFile().isDirectory) continue

            try {
                directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
            } catch (_: ClosedWatchServiceException) {
                return false
            } catch (exception: Exception) {
                logger.log(Level.WARNING, "Could not watch $directory again", exception)
                continue
            }

            val diff = reload()
            if (diff.added.isNotEmpty() || diff.changed.isNotEmpty() || diff.removed.isNotEmpty()) {
                externalChangeListener(diff)
            }

            return true
        }

        return false
    }

    fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
        pendingLoads.values.forEach { it.cancel() }
        pendingLoads.clear()
        watchService?.close()
        watchService = null
        watchScope?.cancel()
        watchScope = null
    }

    protected open fun onEntityChanged(entity: E) = Unit
    protected open fun onEntityRemoved(entity: E) = Unit

    private fun putEntity(fileName: String, entity: E) {
        cachedEntities.put(fileName, entity)?.let(::forget)
        byId[idOf(entity).lowercase()] = entity
        onEntityChanged(entity)
        notifyChangeListeners()
    }

    private fun removeEntity(fileName: String) {
        ownContentHashes.remove(fileName)
        cachedEntities.remove(fileName)?.let {
            forget(it)
            notifyChangeListeners()
        }
    }

    private fun forget(entity: E) {
        byId.remove(idOf(entity).lowercase(), entity)
        onEntityRemoved(entity)
    }

    private fun notifyChangeListeners() {
        changeListeners.forEach { listener ->
            runCatching { listener() }
                .onFailure { logger.log(Level.WARNING, "Repository change listener failed", it) }
        }
    }

    private fun isOwnDeleteEcho(fileName: String): Boolean {
        val deletedAt = recentOwnDeletes[fileName] ?: return false
        if (System.currentTimeMillis() - deletedAt <= OWN_DELETE_ECHO_WINDOW_MILLIS) return true
        recentOwnDeletes.remove(fileName)

        return false
    }

    private fun scheduleLoad(scope: CoroutineScope, file: File) {
        pendingLoads.compute(file.name) { _, previous ->
            previous?.cancel()
            scope.launch {
                delay(LOAD_DEBOUNCE_MILLIS.milliseconds)
                pendingLoads.remove(file.name)
                if (!file.exists()) return@launch
                if (contentHash(file) == ownContentHashes[file.name]) return@launch

                val previous = cachedEntities[file.name]
                val current = load(file) ?: return@launch
                val diff = when {
                    previous == null -> ReloadDiff(added = listOf(detach(current)))
                    previous == current -> return@launch
                    else -> ReloadDiff(changed = listOf(ReloadDiff.Change(detach(previous), detach(current))))
                }
                externalChangeListener(diff)
            }
        }
    }

    private fun contentHash(file: File): Long = runCatching {
        CRC32().apply { update(file.readBytes()) }.value
    }.getOrDefault(-1L)

    private fun File.isYamlFile(): Boolean = extension == "yml"

    private companion object {
        private const val OWN_DELETE_ECHO_WINDOW_MILLIS = 800L
        private const val LOAD_DEBOUNCE_MILLIS = 150L
        private const val REWATCH_DELAY_MILLIS = 1000L
        private val logger = NpcLog.logger
    }
}
