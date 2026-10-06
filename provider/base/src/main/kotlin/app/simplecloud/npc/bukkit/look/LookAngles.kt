package app.simplecloud.npc.bukkit.look

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

object LookAngles {

    fun anglesTowards(dx: Double, dy: Double, dz: Double): FloatArray {
        val yaw = Math.toDegrees(atan2(-dx, dz)).toFloat()
        val horizontalDistance = sqrt(dx * dx + dz * dz)
        val pitch = Math.toDegrees(-atan2(dy, horizontalDistance)).toFloat()

        return floatArrayOf(yaw, pitch)
    }

    fun angleDelta(a: Float, b: Float): Float {
        var diff = (a - b) % 360F
        if (diff > 180F) diff -= 360F
        if (diff < -180F) diff += 360F

        return abs(diff)
    }
}
