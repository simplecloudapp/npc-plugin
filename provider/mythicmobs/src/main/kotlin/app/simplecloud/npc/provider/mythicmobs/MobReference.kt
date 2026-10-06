package app.simplecloud.npc.provider.mythicmobs

import io.lumine.mythic.core.mobs.ActiveMob
import java.util.UUID

object MobReference {

    fun of(mob: ActiveMob): String = of(mob.type.internalName, mob.uniqueId)

    fun matches(mob: ActiveMob, reference: String): Boolean =
        matches(mob.type.internalName, mob.uniqueId, reference)

    internal fun of(mobType: String, uuid: UUID): String = "$mobType-$uuid"

    internal fun matches(mobType: String, uuid: UUID, reference: String): Boolean =
        reference.equals(of(mobType, uuid), ignoreCase = true) ||
            reference.equals(legacyReference(mobType, uuid), ignoreCase = true)

    private fun legacyReference(mobType: String, uuid: UUID): String =
        "$mobType-${uuid.toString().take(LEGACY_UUID_PREFIX_LENGTH)}"

    private const val LEGACY_UUID_PREFIX_LENGTH = 6
}
