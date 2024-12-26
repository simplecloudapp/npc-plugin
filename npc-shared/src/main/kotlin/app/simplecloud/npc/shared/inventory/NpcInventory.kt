package app.simplecloud.npc.shared.inventory

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig
import app.simplecloud.npc.shared.inventory.placeholder.InventoryPlaceholderHandler
import app.simplecloud.npc.shared.inventory.transform.PaginationTransformHandler
import app.simplecloud.npc.shared.inventory.transform.StaticItemTransformHandler
import app.simplecloud.npc.shared.text
import com.noxcrew.interfaces.interfaces.buildChestInterface
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class NpcInventory(
    private val controllerApi: ControllerApi.Coroutine,
    private val config: InventoryConfig,
    private val placeholderHandler: InventoryPlaceholderHandler
) {

    suspend fun open(player: Player) {
        val inventory = createCombinedInterface()
        inventory.open(player)
    }

    private suspend fun createCombinedInterface() = buildChestInterface {
        rows = config.rows
        initialTitle = text(config.title)

        StaticItemTransformHandler(config).handle(this)
        if (config.pagination?.stateItems?.isNotEmpty() == true) {
            PaginationTransformHandler(controllerApi, placeholderHandler, config).handle(this)
        }
    }

}