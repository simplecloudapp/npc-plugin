package app.simplecloud.npc.shared.repository

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.utils.NpcFileUpdater
import java.io.File
import kotlin.io.path.Path

/**
 * @author Niklas Nieberler
 */

class NpcRepository : WatchableYamlDirectoryRepository<NpcConfig, String>(
    CONFIG_DIRECTORY,
    NpcConfig::class.java
) {

    companion object {
        private val CONFIG_DIRECTORY = Path("plugins/simplecloud-npc")
    }

    override fun save(entity: NpcConfig) {
        save("${entity.id}.yml", entity)
    }

    /**
     * Gets the [NpcConfig] by a npc id
     * @param identifier of the npc
     */
    override fun find(identifier: String): NpcConfig? {
        return findAll().firstOrNull { it.id == identifier }
    }

    override fun watchUpdateEvent(file: File) {
        find(file.nameWithoutExtension)?.let(NpcFileUpdater::invokeFile)
    }

}
