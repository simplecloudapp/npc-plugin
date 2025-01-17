package app.simplecloud.npc.shared.action.handler

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.ActionHandler
import app.simplecloud.npc.shared.action.ActionOptions
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.utils.MessageHelper
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class OpenInventoryActionHandler : ActionHandler {

    override fun handle(player: Player, namespace: NpcNamespace, optionProvider: OptionProvider) {
        val action = ActionOptions.OPEN_INVENTORY
        val inventoryName = optionProvider.getOption(action)
        if (inventoryName.isBlank()) {
            MessageHelper.printOptionNotFoundMessage(Action.OPEN_INVENTORY, action, optionProvider)
            return
        }

        if (namespace.inventoryRepository.get(inventoryName) == null) {
            Bukkit.getLogger().info("[SimpleCloud-NPC] No inventory was found with the name $inventoryName")
            return
        }

        namespace.inventoryManager.openInventory(player, inventoryName)
    }

    override fun getOptions() = listOf(
        ActionOptions.OPEN_INVENTORY
    )

}