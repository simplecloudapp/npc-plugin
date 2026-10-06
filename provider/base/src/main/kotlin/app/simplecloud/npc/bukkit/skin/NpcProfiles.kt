package app.simplecloud.npc.bukkit.skin

import app.simplecloud.npc.core.config.NpcConfig
import java.util.UUID

object NpcProfiles {

    fun uuidFor(config: NpcConfig): UUID =
        UUID.nameUUIDFromBytes("npc:${config.id}:${config.entity.skin.texture.orEmpty()}".toByteArray())

    fun nameFor(id: String): String {
        val normalized = id.lowercase()
        val suffix = Integer.toHexString(normalized.hashCode()).padStart(8, '0').takeLast(4)

        return "npc_${normalized.take(7)}_$suffix".take(16)
    }
}
