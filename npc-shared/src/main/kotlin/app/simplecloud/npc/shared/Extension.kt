package app.simplecloud.npc.shared

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey

/**
 * @author Niklas Nieberler
 */

private val miniMessage = MiniMessage.miniMessage()

val hologramNamespacedKey = NamespacedKey("simplecloud", "npc.hologram")
val createAtNamespacedKey = NamespacedKey("simplecloud", "npc.hologram.createat")

fun text(message: String): Component {
    return miniMessage.deserialize(message)
}

fun sync(function: () -> Unit) {
    val plugin = Bukkit.getPluginManager().getPlugin("SimpleCloud-NPC")!!
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin) {
        function()
    }
}