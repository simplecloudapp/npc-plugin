package app.simplecloud.npc.paper.provider

import app.simplecloud.npc.bukkit.compat.ServerCompat
import app.simplecloud.npc.core.render.NpcRenderer
import app.simplecloud.npc.core.render.Providers
import app.simplecloud.npc.provider.citizens.CitizensRenderer
import app.simplecloud.npc.provider.fancynpcs.FancyNpcsRenderer
import app.simplecloud.npc.provider.mythicmobs.MythicMobsRenderer
import app.simplecloud.npc.provider.standalone.StandaloneNpcRenderer
import app.simplecloud.npc.provider.znpcsplus.ZNpcsPlusRenderer
import org.bukkit.plugin.Plugin

object ProviderCatalog {
    fun descriptors(standalone: StandaloneNpcRenderer): List<ProviderDescriptor> = listOf(
        ProviderDescriptor(Providers.FANCYNPCS, requiredPlugin = "FancyNpcs") { plugin, onInteract ->
            FancyNpcsRenderer(plugin, onInteract)
        },

        ProviderDescriptor(Providers.CITIZENS, requiredPlugin = "Citizens") { plugin, onInteract ->
            CitizensRenderer(plugin, onInteract)
        },

        ProviderDescriptor(Providers.ZNPCSPLUS, requiredPlugin = "ZNPCsPlus") { plugin, onInteract ->
            ZNpcsPlusRenderer(plugin, onInteract)
        },

        ProviderDescriptor(Providers.MYTHICMOBS, requiredPlugin = "MythicMobs") { plugin, onInteract ->
            MythicMobsRenderer(plugin, onInteract)
        },

        ProviderDescriptor(Providers.MANNEQUIN, gate = { ServerCompat.hasMannequin }) { plugin, _ ->
            ReflectiveLoader.load<NpcRenderer>(
                ReflectiveSeams.MANNEQUIN_RENDERER,
                "the Mannequin NPC provider",
                arrayOf(Plugin::class.java),
                arrayOf(plugin),
            )
        },

        ProviderDescriptor(Providers.STANDALONE) { _, _ -> standalone },
    )
}
