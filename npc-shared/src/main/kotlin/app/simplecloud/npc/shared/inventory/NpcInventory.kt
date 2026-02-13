package app.simplecloud.npc.shared.inventory

import app.simplecloud.api.CloudApi
import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig
import app.simplecloud.npc.shared.inventory.transform.PaginationTransformHandler
import app.simplecloud.npc.shared.inventory.transform.StaticItemTransformHandler
import app.simplecloud.npc.shared.text
import app.simplecloud.plugin.api.shared.placeholder.provider.ServerPlaceholderProvider
import com.noxcrew.interfaces.interfaces.buildChestInterface
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class NpcInventory(
    private val cloudApi: CloudApi,
    private val config: InventoryConfig,
    private val placeholderProvider: ServerPlaceholderProvider
) {

    suspend fun open(player: Player) {
        val inventory = createCombinedInterface()
        inventory.open(player)
    }

    private suspend fun createCombinedInterface() = buildChestInterface {
        rows = config.rows
        titleSupplier = {
            text(config.title)
        }

        StaticItemTransformHandler(config).handle(this)
        if (config.pagination?.stateItems?.isNotEmpty() == true) {
            PaginationTransformHandler(cloudApi, placeholderProvider, config).handle(this)
        }
    }

}