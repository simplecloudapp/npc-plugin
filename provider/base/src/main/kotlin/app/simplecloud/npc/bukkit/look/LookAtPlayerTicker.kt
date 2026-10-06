package app.simplecloud.npc.bukkit.look

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityRotation
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class LookAtPlayerTicker {

    private val lastAngles = ConcurrentHashMap<Pair<Int, UUID>, FloatArray>()

    fun tick(entityId: Int, x: Double, y: Double, z: Double, viewers: Iterable<Player>, maxDistance: Double) {
        val playerManager = PacketEvents.getAPI().playerManager
        val maxDistanceSquared = maxDistance * maxDistance

        viewers.forEach { player ->
            val location = player.location
            val dx = location.x - x
            val dy = player.eyeLocation.y - (y + EYE_HEIGHT)
            val dz = location.z - z
            if (dx * dx + dy * dy + dz * dz > maxDistanceSquared) return@forEach

            val angles = LookAngles.anglesTowards(dx, dy, dz)
            val key = entityId to player.uniqueId
            val last = lastAngles[key]
            val unchanged = last != null &&
                LookAngles.angleDelta(last[0], angles[0]) < ANGLE_EPSILON &&
                LookAngles.angleDelta(last[1], angles[1]) < ANGLE_EPSILON
            if (unchanged) return@forEach

            lastAngles[key] = angles

            playerManager.sendPacket(player, WrapperPlayServerEntityHeadLook(entityId, angles[0]))
            playerManager.sendPacket(player, WrapperPlayServerEntityRotation(entityId, angles[0], angles[1], true))
        }
    }

    fun forgetViewer(uuid: UUID) {
        lastAngles.keys.removeIf { it.second == uuid }
    }

    fun forgetEntity(entityId: Int) {
        lastAngles.keys.removeIf { it.first == entityId }
    }

    fun forgetPair(entityId: Int, uuid: UUID) {
        lastAngles.remove(entityId to uuid)
    }

    fun clear() = lastAngles.clear()

    private companion object {
        private const val ANGLE_EPSILON = 1.0F
        private const val EYE_HEIGHT = 1.62
    }
}
