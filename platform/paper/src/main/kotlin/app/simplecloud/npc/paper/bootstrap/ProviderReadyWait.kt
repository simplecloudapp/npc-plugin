package app.simplecloud.npc.paper.bootstrap

import app.simplecloud.npc.common.render.RoutingNpcRenderer
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.function.Consumer

object ProviderReadyWait {

    fun schedule(plugin: Plugin, routing: RoutingNpcRenderer, onReady: () -> Unit) {
        var waitedTicks = 0L
        plugin.server.scheduler.runTaskTimer(
            plugin,
            Consumer<BukkitTask> { task ->
                val notReady = routing.providersNotReady
                if (notReady.isNotEmpty() && waitedTicks < TIMEOUT_TICKS) {
                    waitedTicks += POLL_TICKS
                    return@Consumer
                }
                task.cancel()
                if (notReady.isNotEmpty()) {
                    plugin.logger.warning(
                        "NPC providers ${notReady.joinToString()} did not finish loading their NPCs within " +
                            "${TIMEOUT_TICKS / 20} seconds; continuing without them.",
                    )
                }
                onReady()
            },
            INITIAL_DELAY_TICKS,
            POLL_TICKS,
        )
    }

    private const val INITIAL_DELAY_TICKS = 2L
    private const val POLL_TICKS = 5L
    private const val TIMEOUT_TICKS = 600L
}
