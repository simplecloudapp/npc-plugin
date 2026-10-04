package app.simplecloud.npc.paper.provider

import app.simplecloud.npc.bukkit.hologram.ViewerHologramText
import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.provider.WorldReconciler
import app.simplecloud.npc.common.render.RoutingNpcRenderer
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.render.NpcRenderer
import app.simplecloud.npc.core.render.Providers
import app.simplecloud.npc.provider.standalone.StandaloneNpcRenderer
import app.simplecloud.npc.provider.standalone.hologram.TextDisplayHologramRenderer
import org.bukkit.plugin.Plugin

class ProviderWiring private constructor(
    private val standalone: StandaloneNpcRenderer,
    val holograms: TextDisplayHologramRenderer,
    private val renderers: Map<String, NpcRenderer>,
) {
    val routing = RoutingNpcRenderer(renderers)

    fun showPersonalText(viewerText: ViewerHologramText) {
        holograms.viewerText = viewerText
    }

    fun reconcileWorlds(configs: () -> Collection<NpcConfig>) {
        holograms.knownIdsProvider = { idsOf(configs()) { it.hologram.enabled } }
        val mannequin = renderers[Providers.MANNEQUIN] as? WorldReconciler
        mannequin?.knownIdsProvider = {
            idsOf(configs()) { it.entity.provider.equals(Providers.MANNEQUIN, ignoreCase = true) }
        }
        listOfNotNull(holograms, mannequin).forEach(WorldReconciler::reconcileLoadedWorlds)
        standalone.cleanupStaleHitboxes()
    }

    private fun idsOf(configs: Collection<NpcConfig>, filter: (NpcConfig) -> Boolean): Set<String> =
        configs.filter(filter).mapTo(HashSet()) { it.id.lowercase() }

    companion object {
        fun load(plugin: Plugin, onInteract: OnNpcInteract): ProviderWiring {
            val standalone = StandaloneNpcRenderer(plugin)
            val renderers = ProviderLoader.load(plugin, onInteract, ProviderCatalog.descriptors(standalone))

            return ProviderWiring(standalone, TextDisplayHologramRenderer(plugin).also { it.start() }, renderers)
        }
    }
}
