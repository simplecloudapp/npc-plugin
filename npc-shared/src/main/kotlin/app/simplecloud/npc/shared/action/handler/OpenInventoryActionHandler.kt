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

    override fun handle(player: Player, namespace: NpcNamespace<out Any>, optionProvider: OptionProvider) {
        val action = ActionOptions.OPEN_INVENTORY
        val inventoryName = optionProvider.getOption(action)
        if (inventoryName.isBlank()) {
            MessageHelper.printOptionNotFoundMessage(Action.OPEN_INVENTORY, action, optionProvider)
            return
        }

        val inventoryConfig = namespace.inventoryRepository.get(inventoryName)
        if (inventoryConfig == null) {
            Bukkit.getLogger().info("[SimpleCloud-NPC] No inventory was found with the name $inventoryName")
            return
        }

        // TODO: open inventory
        MessageHelper.executeMessageFromOption(player, optionProvider)
    }

}