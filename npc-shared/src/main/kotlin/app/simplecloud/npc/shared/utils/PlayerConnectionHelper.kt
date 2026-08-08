package app.simplecloud.npc.shared.utils

import app.simplecloud.npc.shared.pluginName
import com.google.common.io.ByteStreams
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

object PlayerConnectionHelper {

    fun sendPlayerToServer(player: Player, serverName: String) {
        val out = ByteStreams.newDataOutput()
        out.writeUTF("Connect")
        out.writeUTF(serverName)
        val plugin = Bukkit.getPluginManager().getPlugin(pluginName)
            ?: throw NullPointerException("failed to find $pluginName plugin")
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
    }

}
