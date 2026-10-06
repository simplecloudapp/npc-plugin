package app.simplecloud.npc.core.hologram

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class HologramLine(
    val text: String = "",
    val billboard: HologramBillboard = HologramBillboard.CENTER,
    val viewRange: Float? = null,
    val shadowRadius: Float? = null,
    val displayHeight: Float? = null,
    val displayWidth: Float? = null,
    val alignment: HologramAlignment = HologramAlignment.CENTER,
    val lineWidth: Int? = null,
    val shadow: Boolean? = null,
    val frames: List<String>? = null,
    @Setting("frame-interval")
    val frameInterval: Double? = null,
    val refresh: Double? = null,
) {
    val texts: List<String> get() = frames?.takeIf { it.isNotEmpty() } ?: listOf(text)

    val rotating: Boolean get() = texts.size > 1

    val shownText: String get() = texts.first()

    val interval: Double get() = (frameInterval ?: DEFAULT_FRAME_INTERVAL).coerceIn(FRAME_INTERVAL_RANGE)

    val refreshSeconds: Double get() = (refresh ?: DEFAULT_REFRESH).coerceIn(REFRESH_RANGE)

    fun frameIndex(millis: Long): Int {
        if (!rotating) return 0

        return ((millis / (interval * 1000).toLong()) % texts.size).toInt()
    }

    fun textAt(millis: Long): String = texts[frameIndex(millis)]

    fun withFrames(updated: List<String>): HologramLine = when {
        updated.size > 1 -> copy(text = updated.first(), frames = updated)
        else -> copy(text = updated.firstOrNull() ?: text, frames = null)
    }

    companion object {
        const val DEFAULT_FRAME_INTERVAL = 3.0
        val FRAME_INTERVAL_RANGE = 0.5..60.0
        const val DEFAULT_REFRESH = 1.0
        val REFRESH_RANGE = 0.25..60.0
    }
}

enum class HologramBillboard {
    FIXED, VERTICAL, HORIZONTAL, CENTER,
}

enum class HologramAlignment {
    CENTER, LEFT, RIGHT,
}
