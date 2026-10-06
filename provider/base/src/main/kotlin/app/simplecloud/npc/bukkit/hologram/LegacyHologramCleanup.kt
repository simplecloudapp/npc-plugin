package app.simplecloud.npc.bukkit.hologram

import app.simplecloud.npc.bukkit.NpcKeys
import app.simplecloud.npc.core.NpcLog
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.EntitiesLoadEvent

class LegacyHologramCleanup : Listener {

    fun sweep() {
        Bukkit.getWorlds().forEach { world -> remove(world, world.entities) }
    }

    @EventHandler
    fun onEntitiesLoad(event: EntitiesLoadEvent) {
        remove(event.world, event.entities)
    }

    private fun remove(world: World, entities: Collection<Entity>) {
        val legacy = entities.filterIsInstance<TextDisplay>()
            .filter { it.persistentDataContainer.has(NpcKeys.HOLOGRAM) }
        if (legacy.isEmpty()) return

        legacy.forEach(TextDisplay::remove)
        NpcLog.logger.info("Removed ${legacy.size} old hologram entities in world '${world.name}'.")
    }
}
