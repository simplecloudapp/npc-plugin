package app.simplecloud.npc.namespace.mythicmobs

import app.simplecloud.npc.namespace.mythicmobs.listener.MythicMobInteractListener
import app.simplecloud.npc.shared.provider.NpcProvider
import app.simplecloud.npc.shared.provider.NpcProviderEvents
import app.simplecloud.npc.shared.provider.NpcProviderType
import app.simplecloud.npc.shared.event.listener.listenEvent
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

class MythicMobsNamespace : NpcProvider {
    override val type = NpcProviderType.MYTHIC_MOBS
    override val pluginName = "MythicMobs"

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin, events: NpcProviderEvents) {
        pluginManager.registerEvents(MythicMobInteractListener(events), plugin)
        listenEvent<MythicMobSpawnEvent>(plugin)
            .addAction { events.spawn(type, MobIdFetcher.fetch(it.mob)) }
        listenEvent<MythicMobDespawnEvent>(plugin)
            .addAction { events.remove(type, MobIdFetcher.fetch(it.mob)) }
    }

    override fun exists(reference: String): Boolean = find(reference) != null

    override fun references(): List<String> = MythicBukkit.inst().mobManager.activeMobs
        .filterNot { it.isDead }
        .map(MobIdFetcher::fetch)
        .distinct()

    override fun location(reference: String): Location? {
        val abstractLocation = find(reference)?.location ?: return null
        val world = Bukkit.getWorld(abstractLocation.world.name) ?: return null
        return Location(
            world,
            abstractLocation.x,
            abstractLocation.y,
            abstractLocation.z,
            abstractLocation.yaw,
            abstractLocation.pitch,
        )
    }

    private fun find(reference: String) = MythicBukkit.inst().mobManager.activeMobs
        .firstOrNull { !it.isDead && MobIdFetcher.fetch(it) == reference }
}
