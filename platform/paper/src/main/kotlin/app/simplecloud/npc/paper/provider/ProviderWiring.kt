package app.simplecloud.npc.paper.provider

import app.simplecloud.npc.bukkit.hologram.LegacyHologramCleanup
import app.simplecloud.npc.bukkit.hologram.PacketHologramRenderer
import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.packet.PacketViewerTracker
import app.simplecloud.npc.bukkit.provider.WorldReconciler
import app.simplecloud.npc.common.render.RoutingNpcRenderer
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.render.NpcRenderer
import app.simplecloud.npc.core.render.Providers
import app.simplecloud.npc.provider.standalone.StandaloneNpcRenderer
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

class ProviderWiring private constructor(
    private val standalone: StandaloneNpcRenderer,
    private val tracker: PacketViewerTracker,
    val holograms: PacketHologramRenderer,
    private val legacyHolograms: LegacyHologramCleanup,
    private val renderers: Map<String, NpcRenderer>,
) {
    val routing = RoutingNpcRenderer(renderers)

    fun reconcileWorlds(configs: () -> Collection<NpcConfig>) {
        legacyHolograms.sweep()
        val mannequin = renderers[Providers.MANNEQUIN] as? WorldReconciler
        mannequin?.knownIdsProvider = {
            configs().filter { it.entity.provider.equals(Providers.MANNEQUIN, ignoreCase = true) }
                .mapTo(HashSet()) { it.id.lowercase() }
        }
        mannequin?.reconcileLoadedWorlds()
        standalone.cleanupStaleHitboxes()
    }

    fun shutdown() {
        holograms.texts.shutdown()
        tracker.stop()
    }

    companion object {
        fun load(plugin: Plugin, onInteract: OnNpcInteract): ProviderWiring {
            val tracker = PacketViewerTracker(plugin).also { it.start() }
            val standalone = StandaloneNpcRenderer(plugin, tracker)
            val renderers = ProviderLoader.load(plugin, onInteract, ProviderCatalog.descriptors(standalone))
            val holograms = PacketHologramRenderer(plugin, tracker).also { it.texts.start() }
            val legacy = LegacyHologramCleanup().also { Bukkit.getPluginManager().registerEvents(it, plugin) }

            return ProviderWiring(standalone, tracker, holograms, legacy, renderers)
        }
    }
}
