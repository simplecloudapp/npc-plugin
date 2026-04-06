package app.simplecloud.npc.shared

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey

/**
 * @author Niklas Nieberler
 */

val hologramNamespacedKey = NamespacedKey("simplecloud", "npc.hologram")
val createAtNamespacedKey = NamespacedKey("simplecloud", "npc.hologram.createat")

fun sync(function: () -> Unit) {
    val plugin = Bukkit.getPluginManager().getPlugin("SimpleCloud-NPC")!!
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin) {
        function()
    }
}