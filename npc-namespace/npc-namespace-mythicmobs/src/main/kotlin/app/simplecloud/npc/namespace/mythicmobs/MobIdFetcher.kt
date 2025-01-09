package app.simplecloud.npc.namespace.mythicmobs

import io.lumine.mythic.core.mobs.ActiveMob

/**
 * @author Niklas Nieberler
 */

object MobIdFetcher {

    fun fetch(mob: ActiveMob): String {
        val id = mob.uniqueId.toString().take(6)
        return "${mob.mobType}-$id"
    }

}