package app.simplecloud.npc.shared.action.handler

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.ActionHandler
import app.simplecloud.npc.shared.action.ActionOptions
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.utils.MessageHelper
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class TransferToServerActionHandler : ActionHandler {

    override fun handle(player: Player, namespace: NpcNamespace<out Any>, optionProvider: OptionProvider) {
        val serverIpAction = ActionOptions.TRANSFER_SERVER_IP
        val serverPortAction = ActionOptions.TRANSFER_SERVER_PORT
        val serverIp = optionProvider.getOption(serverIpAction)
        val serverPort = optionProvider.getOption(serverPortAction)
        if (serverIp.isBlank()) {
            MessageHelper.printOptionNotFoundMessage(Action.TRANSFER_TO_SERVER, serverIpAction, optionProvider)
            return
        }
        player.transfer(serverIp, serverPort.toInt())
    }

}