package app.simplecloud.npc.bukkit.hologram

import app.simplecloud.npc.core.config.NpcConfig

object HologramGeometry {
    const val LINE_SPACING = 0.3

    fun baseHeight(config: NpcConfig): Double = config.hologram.startHeight * config.entity.heightFactor()

    fun lineHeights(config: NpcConfig, lineCount: Int): List<Double> {
        val base = baseHeight(config)

        return List(lineCount) { index -> base + LINE_SPACING * (lineCount - 1 - index) }
    }

    fun topHeight(config: NpcConfig, lineCount: Int): Double = baseHeight(config) + LINE_SPACING * lineCount
}
