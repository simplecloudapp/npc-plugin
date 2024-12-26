package app.simplecloud.npc.shared.action.handler

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.ActionHandler
import app.simplecloud.npc.shared.action.ActionOptions
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.utils.MessageHelper
import app.simplecloud.npc.shared.utils.PlayerConnectionHelper
import com.google.common.io.ByteStreams
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class ConnectToServerActionHandler : ActionHandler {

    override fun handle(player: Player, namespace: NpcNamespace, optionProvider: OptionProvider) {
        val action = ActionOptions.CONNECT_TO_SERVER_NAME
        val serverName = optionProvider.getOption(action)
        if (serverName.isBlank()) {
            MessageHelper.printOptionNotFoundMessage(Action.CONNECT_TO_SERVER, action, optionProvider)
            return
        }
        PlayerConnectionHelper.sendPlayerToServer(player, serverName)
    }

    override fun getOptions() = listOf(
        ActionOptions.CONNECT_TO_SERVER_NAME
    )

}