package app.simplecloud.npc.bukkit.interact

import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent

class RealEntityClickGuard : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onDamage(event: EntityDamageEvent) {
        if (isOurs(event.entity)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteract(event: PlayerInteractEntityEvent) {
        if (isOurs(event.rightClicked)) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteractAt(event: PlayerInteractAtEntityEvent) {
        if (isOurs(event.rightClicked)) event.isCancelled = true
    }

    private fun isOurs(entity: Entity): Boolean = InteractableEntities.isRealEntity(entity.entityId)
}
