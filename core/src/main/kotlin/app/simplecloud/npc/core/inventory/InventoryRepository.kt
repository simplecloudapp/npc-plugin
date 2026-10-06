package app.simplecloud.npc.core.inventory

import app.simplecloud.npc.core.repository.WatchableYamlDirectoryRepository
import java.nio.file.Path

class InventoryRepository(
    directory: Path,
) : WatchableYamlDirectoryRepository<InventoryConfiguration>(
    directory,
    InventoryConfiguration::class.java,
) {
    override fun idOf(entity: InventoryConfiguration): String = entity.id
    override fun detach(entity: InventoryConfiguration): InventoryConfiguration = entity.deepCopy()
}
