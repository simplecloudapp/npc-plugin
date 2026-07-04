package app.simplecloud.npc.shared.repository

import app.simplecloud.plugin.api.shared.config.YamlDirectoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchService

abstract class WatchableYamlDirectoryRepository<E, I>(
    private val directory: Path,
    clazz: Class<E>,
) : YamlDirectoryRepository<E, I>(directory, clazz) {

    private val watchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var watchJob: Job? = null
    private var watchService: WatchService? = null

    fun loadAndWatch(): List<E> {
        val entities = load()
        watchUpdates()
        return entities
    }

    fun watchUpdates() {
        if (watchJob?.isActive == true) {
            return
        }

        directory.toFile().mkdirs()
        val service = FileSystems.getDefault().newWatchService()
        directory.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        watchService = service

        watchJob = watchScope.launch {
            runCatching {
                while (isActive) {
                    val watchKey = service.take()
                    watchKey.pollEvents().forEach { event ->
                        val path = event.context() as? Path ?: return@forEach
                        val file = directory.resolve(path).toFile()
                        if (!file.isYamlFile()) {
                            return@forEach
                        }

                        when (event.kind()) {
                            ENTRY_CREATE -> load(file)
                            ENTRY_MODIFY -> {
                                load(file)
                                watchUpdateEvent(file)
                            }
                            ENTRY_DELETE -> watchDeleteEvent(file)
                        }
                    }

                    if (!watchKey.reset()) {
                        break
                    }
                }
            }.onFailure {
                if (it !is ClosedWatchServiceException) {
                    throw it
                }
            }
        }
    }

    fun stopWatching() {
        watchJob?.cancel()
        watchJob = null
        watchService?.close()
        watchService = null
    }

    protected open fun watchUpdateEvent(file: File) {}

    protected open fun watchDeleteEvent(file: File) {}

    private fun File.isYamlFile(): Boolean {
        return extension == "yml" || extension == "yaml"
    }

}
