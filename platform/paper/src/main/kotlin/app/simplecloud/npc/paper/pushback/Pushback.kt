package app.simplecloud.npc.paper.pushback

import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin

object Pushback {

    fun push(player: Player, playerPosition: Vector, yaw: Float, npc: Vector, strength: Double, vertical: Double) {
        player.velocity = direction(playerPosition, yaw, npc).multiply(strength).setY(vertical)
    }

    fun direction(playerPosition: Vector, yaw: Float, npc: Vector): Vector {
        val away = playerPosition.clone().subtract(npc).setY(0.0)
        if (away.lengthSquared() >= EPSILON) return away.normalize()

        val yawRadians = Math.toRadians(yaw.toDouble())

        return Vector(sin(yawRadians), 0.0, -cos(yawRadians))
    }

    private const val EPSILON = 1.0E-6
}
