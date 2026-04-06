package app.simplecloud.npc.shared.repository

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.utils.NpcFileUpdater
import app.simplecloud.plugin.api.shared.config.YamlDirectoryRepository
import java.io.File
import kotlin.io.path.Path

/**
 * @author Niklas Nieberler
 */

class NpcRepository : YamlDirectoryRepository<NpcConfig, String>(
    Path("plugins/simplecloud-npc"),
    NpcConfig::class.java
) {

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
        val npcConfig = find(file.nameWithoutExtension) ?: return
        NpcFileUpdater.invokeFile(npcConfig)
    }

}