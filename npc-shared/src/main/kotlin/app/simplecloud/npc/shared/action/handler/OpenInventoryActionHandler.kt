package app.simplecloud.npc.shared.action.handler

import app.simplecloud.npc.shared.action.ActionHandler
import app.simplecloud.npc.shared.option.OptionProvider
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class OpenInventoryActionHandler : ActionHandler {

    override fun handle(player: Player, optionProvider: OptionProvider) {
        //optionProvider.hasOption()
        //TODO: aus inventories/<inventory.name> schauen
        player.sendMessage("hallo was geht")
    }

}