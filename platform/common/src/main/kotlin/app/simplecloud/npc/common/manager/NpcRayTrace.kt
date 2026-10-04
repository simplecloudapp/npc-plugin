package app.simplecloud.npc.common.manager

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.repository.NpcRepository
import kotlin.math.cos
import kotlin.math.sin

object NpcRayTrace {
    private const val MAX_REACH = 6.0
    private const val HIT_RADIUS = 0.6
    private const val EYE_HEIGHT = 1.62
    private const val NPC_CENTER_HEIGHT = 1.1

    fun findLookedAt(repository: NpcRepository, player: NpcPlayer): NpcConfig? {
        val eye = player.location()
        val originY = eye.y + EYE_HEIGHT
        val direction = directionOf(eye.yaw, eye.pitch)

        return repository.findAll()
            .asSequence()
            .filter { it.entity.location.world.equals(eye.world, true) }
            .mapNotNull { config ->
                rayHitDistance(eye.x, originY, eye.z, direction, config)?.let { config to it }
            }
            .minByOrNull { it.second }
            ?.first
    }

    private fun rayHitDistance(
        originX: Double,
        originY: Double,
        originZ: Double,
        direction: DoubleArray,
        config: NpcConfig,
    ): Double? {
        val location = config.entity.location
        val centerX = location.x
        val centerY = location.y + NPC_CENTER_HEIGHT
        val centerZ = location.z

        val toX = centerX - originX
        val toY = centerY - originY
        val toZ = centerZ - originZ

        val t = toX * direction[0] + toY * direction[1] + toZ * direction[2]
        if (t !in 0.0..MAX_REACH) return null

        val closestX = originX + direction[0] * t
        val closestY = originY + direction[1] * t
        val closestZ = originZ + direction[2] * t

        val dx = closestX - centerX
        val dy = closestY - centerY
        val dz = closestZ - centerZ
        val distanceSquared = dx * dx + dy * dy + dz * dz

        return if (distanceSquared <= HIT_RADIUS * HIT_RADIUS) t else null
    }

    private fun directionOf(yaw: Float, pitch: Float): DoubleArray {
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())

        return doubleArrayOf(
            -sin(yawRad) * cos(pitchRad),
            -sin(pitchRad),
            cos(yawRad) * cos(pitchRad),
        )
    }
}
