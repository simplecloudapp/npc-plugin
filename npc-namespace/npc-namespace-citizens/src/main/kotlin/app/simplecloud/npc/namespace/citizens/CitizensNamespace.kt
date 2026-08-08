package app.simplecloud.npc.namespace.citizens

import app.simplecloud.npc.namespace.citizens.listener.NpcLeftClickListener
import app.simplecloud.npc.namespace.citizens.listener.NpcRightClickListener
import app.simplecloud.npc.shared.provider.NpcProvider
import app.simplecloud.npc.shared.provider.NpcCreationDefaults
import app.simplecloud.npc.shared.provider.NpcProviderEvents
import app.simplecloud.npc.shared.provider.NpcProviderType
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.event.NPCDespawnEvent
import net.citizensnpcs.api.event.NPCRemoveEvent
import net.citizensnpcs.api.event.NPCSpawnEvent
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.trait.LookClose
import net.citizensnpcs.trait.MirrorTrait
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class CitizensNamespace : NpcProvider {

    override val type = NpcProviderType.CITIZENS
    override val pluginName = "Citizens"
    override val supportsCreation = true

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin, events: NpcProviderEvents) {
        pluginManager.registerEvents(NpcRightClickListener(events), plugin)
        pluginManager.registerEvents(NpcLeftClickListener(events), plugin)
        pluginManager.registerEvents(LifecycleListener(events), plugin)
    }

    override fun exists(reference: String): Boolean = find(reference) != null

    override fun references(): List<String> = CitizensAPI.getNPCRegistry()
        .map { it.id.toString() }

    override fun location(reference: String): Location? = find(reference)?.storedLocation

    override fun create(id: String, player: Player, location: Location): String {
        val registry = CitizensAPI.getNPCRegistry()
        val npc = registry.createNPC(EntityType.PLAYER, id, location)
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false)
        npc.getOrAddTrait(LookClose::class.java).apply {
            lookClose(true)
            setPerPlayer(true)
            setRange(NpcCreationDefaults.LOOK_AT_PLAYER_DISTANCE.toDouble())
        }
        npc.getOrAddTrait(MirrorTrait::class.java).setEnabled(true)
        registry.saveToStore()
        return npc.id.toString()
    }

    override fun delete(reference: String): Boolean {
        val npc = find(reference) ?: return false
        npc.destroy()
        CitizensAPI.getNPCRegistry().saveToStore()
        return true
    }

    private fun find(reference: String): NPC? {
        return reference.toIntOrNull()?.let(CitizensAPI.getNPCRegistry()::getById)
    }

    private class LifecycleListener(private val events: NpcProviderEvents) : Listener {
        @EventHandler
        fun onSpawn(event: NPCSpawnEvent) = events.spawn(NpcProviderType.CITIZENS, event.npc.id.toString())

        @EventHandler
        fun onDespawn(event: NPCDespawnEvent) = events.remove(NpcProviderType.CITIZENS, event.npc.id.toString())

        @EventHandler
        fun onRemove(event: NPCRemoveEvent) = events.remove(NpcProviderType.CITIZENS, event.npc.id.toString())
    }
}
