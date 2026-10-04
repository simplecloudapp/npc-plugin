package app.simplecloud.npc.bukkit.scheduling

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException

fun <T> Plugin.sync(function: () -> T): T {
    if (Bukkit.isPrimaryThread()) return function()
    if (!isEnabled) throw CancellationException("Plugin $name is disabled")

    return Bukkit.getScheduler().callSyncMethod(this, Callable(function)).get()
}
