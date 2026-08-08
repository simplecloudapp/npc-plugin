package app.simplecloud.npc.plugin.paper.namespace

import app.simplecloud.npc.namespace.citizens.CitizensNamespace
import app.simplecloud.npc.namespace.fancynpcs.FancyNpcsNamespace
import app.simplecloud.npc.namespace.mythicmobs.MythicMobsNamespace
import app.simplecloud.npc.namespace.znpcsplus.ZNpcsPlusNamespace
import app.simplecloud.npc.shared.provider.NpcProviderRegistry

object NamespaceService {
    fun createProviderRegistry() = NpcProviderRegistry(
        listOf(
            CitizensNamespace(),
            FancyNpcsNamespace(),
            MythicMobsNamespace(),
            ZNpcsPlusNamespace(),
        )
    )
}
