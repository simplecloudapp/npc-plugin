package app.simplecloud.npc.core.location

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import kotlin.math.cos
import kotlin.math.sin

@ConfigSerializable
data class NpcLocation(
    val world: String = "world",
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0,
    val yaw: Float = 0F,
    val pitch: Float = 0F,
) {
    fun inFrontLookingBack(distance: Double): NpcLocation {
        val radians = Math.toRadians(yaw.toDouble())
        return copy(
            x = x - sin(radians) * distance,
            z = z + cos(radians) * distance,
            yaw = ((yaw + 180f) % 360f + 360f) % 360f,
            pitch = 0f,
        )
    }
}
