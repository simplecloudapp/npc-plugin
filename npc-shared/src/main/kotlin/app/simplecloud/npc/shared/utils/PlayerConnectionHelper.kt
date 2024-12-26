package app.simplecloud.npc.shared.utils

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
        val plugin = Bukkit.getPluginManager().getPlugin("SimpleCloud-NPC")
            ?: throw NullPointerException("failed to find SimpleCloud-NPC plugin")
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
    }

}