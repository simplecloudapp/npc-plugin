package app.simplecloud.npc.shared

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import java.util.concurrent.Callable

/**
 * @author Niklas Nieberler
 */

val hologramNamespacedKey = NamespacedKey("simplecloud", "npc.hologram")
val createAtNamespacedKey = NamespacedKey("simplecloud", "npc.hologram.createat")
val hologramLineNamespacedKey = NamespacedKey("simplecloud", "npc.hologram.line")
const val pluginName = "simplecloud-npc"

fun <T> sync(function: () -> T): T {
    if (Bukkit.isPrimaryThread()) return function()
    val plugin = Bukkit.getPluginManager().getPlugin(pluginName)!!
    return Bukkit.getScheduler().callSyncMethod(plugin, Callable(function)).get()
}
