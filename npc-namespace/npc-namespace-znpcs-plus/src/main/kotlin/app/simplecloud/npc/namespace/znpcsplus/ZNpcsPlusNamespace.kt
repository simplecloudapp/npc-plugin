package app.simplecloud.npc.namespace.znpcsplus

import app.simplecloud.npc.namespace.znpcsplus.listener.NpcInteractListener
import app.simplecloud.npc.shared.provider.NpcCreationDefaults
import app.simplecloud.npc.shared.provider.NpcProvider
import app.simplecloud.npc.shared.provider.NpcProviderEvents
import app.simplecloud.npc.shared.provider.NpcProviderType
import app.simplecloud.npc.shared.event.listener.listenEvent
import lol.pyr.znpcsplus.api.NpcApiProvider
import lol.pyr.znpcsplus.api.event.NpcDespawnEvent
import lol.pyr.znpcsplus.api.event.NpcSpawnEvent
import lol.pyr.znpcsplus.api.skin.SkinDescriptor
import lol.pyr.znpcsplus.util.LookType
import lol.pyr.znpcsplus.util.NpcLocation
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

class ZNpcsPlusNamespace : NpcProvider {
    override val type = NpcProviderType.ZNPCS_PLUS
    override val pluginName = "ZNPCsPlus"
    override val supportsCreation = true

    override fun registerListeners(pluginManager: PluginManager, plugin: Plugin, events: NpcProviderEvents) {
        pluginManager.registerEvents(NpcInteractListener(events), plugin)
        listenEvent<NpcSpawnEvent>(plugin).addAction { events.spawn(type, it.entry.id) }
        listenEvent<NpcDespawnEvent>(plugin).addAction { events.remove(type, it.entry.id) }
    }

    override fun exists(reference: String): Boolean = NpcApiProvider.get().npcRegistry.getById(reference) != null

    override fun references(): Collection<String> = NpcApiProvider.get().npcRegistry.allIds

    override fun location(reference: String): Location? {
        val npc = NpcApiProvider.get().npcRegistry.getById(reference)?.npc ?: return null
        return npc.location.toBukkitLocation(npc.world)
    }

    override fun create(id: String, player: Player, location: Location): String {
        val api = NpcApiProvider.get()
        val registry = api.npcRegistry
        require(registry.getById(id) == null) { "ZNPCsPlus NPC $id already exists" }

        val playerType = checkNotNull(api.npcTypeRegistry.getByName("player")) {
            "ZNPCsPlus player NPC type is unavailable"
        }
        val entry = registry.create(id, location.world, playerType, NpcLocation(location))

        return try {
            val npc = entry.npc
            npc.setProperty(
                checkNotNull(api.propertyRegistry.getByName("look", LookType::class.java)) {
                    "ZNPCsPlus look property is unavailable"
                },
                LookType.PER_PLAYER,
            )
            npc.setProperty(
                checkNotNull(api.propertyRegistry.getByName("look_distance", Double::class.java)) {
                    "ZNPCsPlus look-distance property is unavailable"
                },
                NpcCreationDefaults.LOOK_AT_PLAYER_DISTANCE.toDouble(),
            )
            npc.setProperty(
                checkNotNull(api.propertyRegistry.getByName("skin", SkinDescriptor::class.java)) {
                    "ZNPCsPlus skin property is unavailable"
                },
                api.skinDescriptorFactory.createMirrorDescriptor(),
            )
            entry.enableEverything()
            registry.save()
            entry.id
        } catch (exception: Exception) {
            registry.delete(entry.id)
            throw exception
        }
    }

    override fun delete(reference: String): Boolean {
        val registry = NpcApiProvider.get().npcRegistry
        if (registry.getById(reference) == null) return false
        registry.delete(reference)
        registry.save()
        return registry.getById(reference) == null
    }
}
