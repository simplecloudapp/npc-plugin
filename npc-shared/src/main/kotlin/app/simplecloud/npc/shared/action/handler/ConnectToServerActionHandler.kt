package app.simplecloud.npc.shared.action.handler

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.ActionHandler
import app.simplecloud.npc.shared.action.ActionOptions
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.utils.MessageHelper
import com.google.common.io.ByteStreams
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class ConnectToServerActionHandler : ActionHandler {

    override fun handle(player: Player, optionProvider: OptionProvider) {
        val action = ActionOptions.CONNECT_TO_SERVER_NAME
        val serverName = optionProvider.getOption(action)
        if (serverName.isBlank()) {
            MessageHelper.printOptionNotFoundMessage(Action.CONNECT_TO_SERVER, action, optionProvider)
            return
        }
        sendPlayerToServer(player, serverName)
        MessageHelper.executeMessageFromOption(player, optionProvider)
    }

    private fun sendPlayerToServer(player: Player, serverName: String) {
        val out = ByteStreams.newDataOutput()
        out.writeUTF("Connect")
        out.writeUTF(serverName)
        val plugin = Bukkit.getPluginManager().getPlugin("SimpleCloud-NPC")
            ?: throw NullPointerException("failed to find SimpleCloud-NPC plugin")
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
    }

}