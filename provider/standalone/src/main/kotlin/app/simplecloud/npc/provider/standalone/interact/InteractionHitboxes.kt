package app.simplecloud.npc.provider.standalone.interact

import app.simplecloud.npc.bukkit.NpcKeys
import app.simplecloud.npc.bukkit.interact.InteractableEntities
import app.simplecloud.npc.bukkit.location.toBukkitLocation
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.config.NpcPose
import org.bukkit.Bukkit
import org.bukkit.entity.Interaction
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.world.EntitiesLoadEvent
import org.bukkit.event.world.EntitiesUnloadEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InteractionHitboxes(
    private val plugin: Plugin,
) : Listener {

    private val idByHitboxId = ConcurrentHashMap<UUID, String>()

    fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun onDisable() {
        HandlerList.unregisterAll(this)

        idByHitboxId.keys.toList().forEach(::remove)
        idByHitboxId.clear()
    }

    @EventHandler
    fun onEntitiesLoad(event: EntitiesLoadEvent) {
        event.entities.filterIsInstance<Interaction>()
            .filter { it.persistentDataContainer.has(NpcKeys.HITBOX, PersistentDataType.STRING) }
            .forEach { hitbox ->
                val npcId = idByHitboxId[hitbox.uniqueId]
                if (npcId == null) {
                    hitbox.remove()
                } else {
                    InteractableEntities.register(hitbox.entityId, npcId, realEntity = true, hitbox = true)
                }
            }
    }

    @EventHandler
    fun onEntitiesUnload(event: EntitiesUnloadEvent) {
        event.entities.filterIsInstance<Interaction>()
            .filter { idByHitboxId.containsKey(it.uniqueId) }
            .forEach { InteractableEntities.unregister(it.entityId) }
    }

    fun spawn(config: NpcConfig): UUID? {
        val world = Bukkit.getWorld(config.entity.location.world) ?: return null
        val lying = config.entity.pose == NpcPose.SWIMMING || config.entity.pose == NpcPose.SLEEPING
        val scale = config.entity.effectiveScale().toFloat()
        val widthFactor = if (lying) LYING_WIDTH_FACTOR else 1F
        val hitbox = world.spawn(config.entity.location.toBukkitLocation(world), Interaction::class.java).apply {
            interactionWidth = HITBOX_WIDTH * scale * widthFactor
            interactionHeight = HITBOX_HEIGHT * config.entity.heightFactor().toFloat()
            isResponsive = true
            setGravity(false)
            persistentDataContainer.set(NpcKeys.HITBOX, PersistentDataType.STRING, config.id)
        }

        idByHitboxId[hitbox.uniqueId] = config.id
        InteractableEntities.register(hitbox.entityId, config.id, realEntity = true, hitbox = true)

        return hitbox.uniqueId
    }

    fun teleport(config: NpcConfig, hitboxId: UUID) {
        val world = Bukkit.getWorld(config.entity.location.world) ?: return
        (Bukkit.getEntity(hitboxId) as? Interaction)?.teleport(config.entity.location.toBukkitLocation(world))
    }

    fun remove(hitboxId: UUID) {
        idByHitboxId.remove(hitboxId)
        (Bukkit.getEntity(hitboxId) as? Interaction)?.let { hitbox ->
            InteractableEntities.unregister(hitbox.entityId)
            hitbox.remove()
        }
    }

    fun cleanupStale() {
        Bukkit.getWorlds().forEach { world ->
            world.entities.filterIsInstance<Interaction>()
                .filter { it.persistentDataContainer.has(NpcKeys.HITBOX, PersistentDataType.STRING) }
                .filterNot { idByHitboxId.containsKey(it.uniqueId) }
                .forEach(Interaction::remove)
        }
    }

    private companion object {
        private const val HITBOX_WIDTH = 0.6F
        private const val HITBOX_HEIGHT = 1.8F
        private const val LYING_WIDTH_FACTOR = 3F
    }
}
