package app.simplecloud.npc.bukkit.interact

import java.util.concurrent.ConcurrentHashMap

object InteractableEntities {

    class Target(val npcId: String, val realEntity: Boolean, val hitbox: Boolean)

    private val byEntityId = ConcurrentHashMap<Int, Target>()

    fun register(entityId: Int, npcId: String, realEntity: Boolean, hitbox: Boolean = false) {
        byEntityId[entityId] = Target(npcId, realEntity, hitbox)
    }

    fun unregister(entityId: Int) {
        byEntityId.remove(entityId)
    }

    fun find(entityId: Int): Target? = byEntityId[entityId]

    fun bodiesOf(npcId: String): List<Int> =
        byEntityId.filterValues { it.npcId == npcId && !it.hitbox }.keys.toList()

    fun isRealEntity(entityId: Int): Boolean = byEntityId[entityId]?.realEntity == true

    fun clear() = byEntityId.clear()
}
