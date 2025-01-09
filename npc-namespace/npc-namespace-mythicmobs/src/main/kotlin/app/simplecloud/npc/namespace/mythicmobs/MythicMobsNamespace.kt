package app.simplecloud.npc.namespace.mythicmobs

import app.simplecloud.npc.namespace.mythicmobs.listener.*
import app.simplecloud.npc.shared.event.EventActionType
import app.simplecloud.npc.shared.event.registerActionEvent
import app.simplecloud.npc.shared.namespace.NpcNamespace
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent
import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

class MythicMobsNamespace : NpcNamespace(
    "MythicMobs"
) {

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin) {
        pluginManager.registerEvents(MythicMobInteractListener(this), plugin)
        eventManager.registerActionEvent<MythicMobSpawnEvent>(plugin, EventActionType.SPAWN, { MobIdFetcher.fetch(it.mob) })
        eventManager.registerActionEvent<MythicMobDespawnEvent>(plugin, EventActionType.REMOVE, { MobIdFetcher.fetch(it.mob) })
    }

    override fun findAllNpcs(): List<String> {
        return MythicBukkit.inst().mobManager.activeMobs
            .filter { !it.isDead }
            .map { MobIdFetcher.fetch(it) }
    }

    override fun findLocationByNpc(id: String): Location? {
        val abstractLocation = MythicBukkit.inst().mobManager.activeMobs
            .firstOrNull { MobIdFetcher.fetch(it) == id }?.location ?: return null
        val world = Bukkit.getWorld(abstractLocation.world.name)
            ?: throw NullPointerException("failed to find world ${abstractLocation.world.name}")
        return Location(
            world,
            abstractLocation.x,
            abstractLocation.y,
            abstractLocation.z,
            abstractLocation.yaw,
            abstractLocation.pitch
        )
    }

}