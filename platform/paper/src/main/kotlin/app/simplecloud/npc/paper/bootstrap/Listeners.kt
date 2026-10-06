package app.simplecloud.npc.paper.bootstrap

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

object Listeners {
    fun register(plugin: Plugin, vararg listeners: Any) {
        listeners.forEach { listener ->
            (listener as? Listener)?.let { plugin.server.pluginManager.registerEvents(it, plugin) }
            (listener as? PacketListenerAbstract)?.let { PacketEvents.getAPI().eventManager.registerListener(it) }
        }
    }
}
