package app.simplecloud.npc.namespace.fancynpcs

import app.simplecloud.npc.namespace.fancynpcs.listener.NpcInteractListener
import app.simplecloud.npc.shared.provider.NpcProvider
import app.simplecloud.npc.shared.provider.NpcCreationDefaults
import app.simplecloud.npc.shared.provider.NpcProviderEvents
import app.simplecloud.npc.shared.provider.NpcProviderType
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import de.oliver.fancynpcs.api.Npc
import de.oliver.fancynpcs.api.NpcData
import de.oliver.fancynpcs.api.events.NpcCreateEvent
import de.oliver.fancynpcs.api.events.NpcRemoveEvent
import de.oliver.fancynpcs.api.events.NpcSpawnEvent
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class FancyNpcsNamespace : NpcProvider {

    override val type = NpcProviderType.FANCY_NPCS
    override val pluginName = "FancyNpcs"
    override val supportsCreation = true

    private lateinit var fancyNpcPlugin: FancyNpcsPlugin

    override fun onEnable() {
        fancyNpcPlugin = FancyNpcsPlugin.get()
    }

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin, events: NpcProviderEvents) {
        pluginManager.registerEvents(NpcInteractListener(events), plugin)
        pluginManager.registerEvents(LifecycleListener(events), plugin)
    }

    override fun exists(reference: String): Boolean = find(reference) != null

    override fun references(): List<String> = fancyNpcPlugin.npcManager.allNpcs
        .map { it.data.id }

    override fun location(reference: String): Location? = find(reference)?.data?.location

    override fun create(id: String, player: Player, location: Location): String {
        require(fancyNpcPlugin.npcManager.getNpc(id) == null) { "FancyNPC $id already exists" }

        val npcData = NpcData(id, player.uniqueId, location).apply {
            setDisplayName("<empty>")
            setTurnToPlayer(true)
            setTurnToPlayerDistance(NpcCreationDefaults.LOOK_AT_PLAYER_DISTANCE)
            setMirrorSkin(true)
        }
        val npc = fancyNpcPlugin.npcAdapter.apply(npcData)
        check(NpcCreateEvent(npc, player).callEvent()) { "FancyNPC creation was cancelled" }
        npc.create()
        fancyNpcPlugin.npcManager.registerNpc(npc)
        npc.spawnForAll()
        fancyNpcPlugin.npcManager.saveNpcs(true)
        return npc.data.id
    }

    override fun delete(reference: String): Boolean {
        val npc = find(reference) ?: return false
        npc.removeForAll()
        fancyNpcPlugin.npcManager.removeNpc(npc)
        fancyNpcPlugin.npcManager.saveNpcs(true)
        return true
    }

    private fun find(reference: String): Npc? = fancyNpcPlugin.npcManager.getNpcById(reference)

    private class LifecycleListener(private val events: NpcProviderEvents) : Listener {
        @EventHandler
        fun onSpawn(event: NpcSpawnEvent) = events.spawn(NpcProviderType.FANCY_NPCS, event.npc.data.id)

        @EventHandler
        fun onRemove(event: NpcRemoveEvent) = events.remove(NpcProviderType.FANCY_NPCS, event.npc.data.id)
    }
}
