package app.simplecloud.npc.shared.inventory

import app.simplecloud.npc.shared.controller.ControllerService
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.plugin.api.shared.placeholder.PlaceholderProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class InventoryManager(
    private val namespace: NpcNamespace
) {

    private val placeholderProvider = PlaceholderProvider.serverPlaceholderProvider

    /**
     * Opens an inventory by a config
     * @param player to open
     * @param id of the inventory
     */
    fun openInventory(player: Player, id: String) {
        val config = this.namespace.inventoryRepository.get(id) ?: return
        CoroutineScope(Dispatchers.IO).launch {
            NpcInventory(ControllerService.controllerApi, config, placeholderProvider).open(player)
        }
    }

}