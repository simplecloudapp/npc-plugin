package app.simplecloud.npc.shared.repository

import app.simplecloud.npc.shared.config.NpcConfig
import kotlin.io.path.Path

/**
 * @author Niklas Nieberler
 */

class NpcRepository : YamlDirectoryRepository<NpcConfig>(
    Path("plugins/npcs"),
    NpcConfig::class.java
) {

    /**
     * Gets the [NpcConfig] by a npc id
     * @param id of the npc
     */
    fun get(id: String): NpcConfig? {
        return getAll().firstOrNull { it.id == id }
    }

}