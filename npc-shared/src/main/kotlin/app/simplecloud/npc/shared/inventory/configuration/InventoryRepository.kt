package app.simplecloud.npc.shared.inventory.configuration

import app.simplecloud.npc.shared.repository.YamlDirectoryRepository
import kotlin.io.path.Path

/**
 * @author Niklas Nieberler
 */

class InventoryRepository : YamlDirectoryRepository<InventoryConfig>(
    Path("plugins/npcs/inventories"),
    InventoryConfig::class.java
) {

    /**
     * Gets the [InventoryConfig] by a npc id
     * @param id of the npc
     */
    fun get(id: String): InventoryConfig? {
        return getAll().firstOrNull { it.id == id }
    }

}