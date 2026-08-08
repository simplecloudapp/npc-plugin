package app.simplecloud.npc.shared.repository

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.config.NpcConfigMigration
import app.simplecloud.npc.shared.provider.NpcProviderType
import app.simplecloud.npc.shared.provider.NpcProviderRegistry
import app.simplecloud.npc.shared.utils.NpcFileUpdater
import java.io.File
import java.nio.file.Path

/**
 * @author Niklas Nieberler
 */

class NpcRepository(
    private val directory: Path,
    providerRegistry: NpcProviderRegistry,
) : WatchableYamlDirectoryRepository<NpcConfig, String>(
    directory,
    NpcConfig::class.java,
    NpcConfigMigration.migrator(providerRegistry),
) {
    var externalDeleteListener: (NpcConfig) -> Unit = {}

    override fun save(entity: NpcConfig) {
        save("${entity.id}.yml", entity.normalize())
    }

    /**
     * Gets the [NpcConfig] by a npc id
     * @param identifier of the npc
     */
    override fun find(identifier: String): NpcConfig? {
        return findAll().firstOrNull { it.id.equals(identifier, true) }
    }

    /**
     * Resolves the user-facing SimpleCloud id or an unambiguous provider reference.
     * This lets commands accept familiar provider names such as a ZNPCsPlus id.
     */
    fun findBySelector(selector: String): NpcConfig? {
        find(selector)?.let { return it }
        return findAll()
            .filter { it.provider.reference.equals(selector, true) }
            .singleOrNull()
    }

    fun selectors(): List<String> {
        val configs = findAll()
        val uniqueReferences = configs
            .groupBy { it.provider.reference.lowercase() }
            .values
            .filter { it.size == 1 }
            .map { it.single().provider.reference }
        return (configs.map { it.id } + uniqueReferences)
            .distinctBy { it.lowercase() }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    fun findByProvider(type: NpcProviderType, reference: String): NpcConfig? {
        return findAll().firstOrNull {
            it.provider.type == type && it.provider.reference.equals(reference, true)
        }
    }

    fun reload(): List<NpcConfig> {
        findAll().filter { config ->
            !directory.resolve("${config.id}.yml").toFile().exists() &&
                !directory.resolve("${config.id}.yaml").toFile().exists()
        }.forEach {
            externalDeleteListener(it)
            delete(it)
        }
        return load()
    }

    override fun watchUpdateEvent(file: File) {
        find(file.nameWithoutExtension)?.let(NpcFileUpdater::invokeFile)
    }

    override fun watchDeleteEvent(file: File) {
        find(file.nameWithoutExtension)?.let {
            externalDeleteListener(it)
            delete(it)
        }
    }

}
