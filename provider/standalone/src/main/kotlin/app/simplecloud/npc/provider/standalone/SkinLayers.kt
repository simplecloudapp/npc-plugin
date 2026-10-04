package app.simplecloud.npc.provider.standalone

import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import me.tofaa.entitylib.meta.types.AvatarMeta
import me.tofaa.entitylib.meta.types.LivingEntityMeta
import me.tofaa.entitylib.meta.types.PlayerMeta

internal object SkinLayers {

    private const val ALL: Byte = 0x7F

    fun showAll(meta: PlayerMeta, server: ServerVersion) {
        meta.setIndex(indexFor(server), EntityDataTypes.BYTE, ALL)
    }

    fun indexFor(server: ServerVersion): Byte =
        if (server.isNewerThanOrEquals(ServerVersion.V_1_21_9)) {
            (AvatarMeta.OFFSET + 1).toByte()
        } else {
            (LivingEntityMeta.MAX_OFFSET + 2).toByte()
        }
}
