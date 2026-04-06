package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.bridge.ServerBridge
import app.simplecloud.plugin.api.shared.placeholder.PlaceholderProvider
import net.kyori.adventure.text.Component

/**
 * @author Niklas Nieberler
 */

object HologramPlaceholderHelper {

    suspend fun appendPlaceholder(serverBridge: ServerBridge, text: String): Component {
        return when {
            serverBridge.isPersistentServer -> {
                Component.text("Persistent server placeholders are not supported.")
            }

            serverBridge.isGroup -> {
                val group = requireNotNull(serverBridge.getGroup()) { "Failed to find group for $serverBridge" }
                PlaceholderProvider.groupPlaceholderProvider.append(group, text, "group")
            }

            serverBridge.isServer -> {
                val server = requireNotNull(serverBridge.getServer()) { "Failed to find server for $serverBridge" }
                PlaceholderProvider.serverPlaceholderProvider.append(server, text, "server")
            }

            else -> throw IllegalStateException("no server placeholder found for $serverBridge")
        }
    }
}