package app.simplecloud.npc.core.repository

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.config.migration.NpcConfigMigration
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class NpcRepository(
    directory: Path,
) : WatchableYamlDirectoryRepository<NpcConfig>(
    directory,
    NpcConfig::class.java,
    NpcConfigMigration.migrator(),
    NpcConfigMigration::stampCurrentLayout,
) {
    private val byProviderReference = ConcurrentHashMap<String, NpcConfig>()

    override fun idOf(entity: NpcConfig): String = entity.id

    override fun detach(entity: NpcConfig): NpcConfig = entity.deepCopy()

    override fun beforeSave(entity: NpcConfig) {
        entity.normalize()
    }

    fun findByProvider(provider: String, reference: String): NpcConfig? =
        byProviderReference[providerKey(provider, reference)]?.deepCopy()

    override fun onEntityChanged(entity: NpcConfig) {
        byProviderReference.entries.removeIf { it.value.id.equals(entity.id, true) }
        val reference = entity.entity.providerReference ?: return
        byProviderReference[providerKey(entity.entity.provider, reference)] = entity
    }

    override fun onEntityRemoved(entity: NpcConfig) {
        val reference = entity.entity.providerReference ?: return
        byProviderReference.remove(providerKey(entity.entity.provider, reference), entity)
    }

    private fun providerKey(provider: String, reference: String) = "${provider.lowercase()}|$reference"
}
