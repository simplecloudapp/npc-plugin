package app.simplecloud.npc.shared.repository

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.utils.NpcFileUpdater
import java.io.File
import kotlin.io.path.Path

/**
 * @author Niklas Nieberler
 */

class NpcRepository : YamlDirectoryRepository<NpcConfig>(
    Path("plugins/simplecloud-npc"),
    NpcConfig::class.java
) {

    /**
     * Gets the [NpcConfig] by a npc id
     * @param id of the npc
     */
    fun get(id: String): NpcConfig? {
        return getAll().firstOrNull { it.id == id }
    }

    override fun watchUpdateEvent(file: File) {
        val npcConfig = get(file.nameWithoutExtension)
        if (npcConfig == null)
            return
        NpcFileUpdater.invokeFile(npcConfig)
    }

}