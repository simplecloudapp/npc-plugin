package app.simplecloud.npc.provider.mythicmobs

import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.provider.LinkedProviderRenderer
import app.simplecloud.npc.core.location.NpcLocation
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.mobs.ActiveMob
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class MythicMobsRenderer(
    plugin: Plugin,
    private val onInteract: OnNpcInteract,
) : LinkedProviderRenderer<ActiveMob>(plugin, "MythicMobs") {

    override val supportsCreation: Boolean = false

    override fun createListener(): Listener = MythicMobsInteractListener(::configIdForEventKey, onInteract)

    override fun linkCandidates(): List<Pair<String, ActiveMob>> =
        MythicBukkit.inst().mobManager.activeMobs.filterNot { it.isDead }.map { MobReference.of(it) to it }

    override fun eventKeyOf(tracked: ActiveMob): String = MobReference.of(tracked)

    override fun referenceOf(tracked: ActiveMob): String = MobReference.of(tracked)

    override fun findByReference(reference: String): ActiveMob? = MythicBukkit.inst().mobManager.activeMobs
        .firstOrNull { !it.isDead && MobReference.matches(it, reference) }

    override fun locationOf(tracked: ActiveMob): NpcLocation? = tracked.location?.let {
        NpcLocation(it.world.name, it.x, it.y, it.z, it.yaw, it.pitch)
    }
}
