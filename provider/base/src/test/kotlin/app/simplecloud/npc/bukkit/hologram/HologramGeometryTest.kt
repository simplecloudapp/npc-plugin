package app.simplecloud.npc.bukkit.hologram

import app.simplecloud.npc.core.config.NpcConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class HologramGeometryTest {

    private val config = NpcConfig(id = "lobby").apply { hologram.startHeight = 2.0 }

    @Test
    fun `first line is on top and the last sits at the start height`() {
        val heights = HologramGeometry.lineHeights(config, 3)

        assertEquals(listOf(2.6, 2.3, 2.0), heights.map { Math.round(it * 100) / 100.0 })
    }

    @Test
    fun `scale moves the whole hologram`() {
        val scaled = config.copy(entity = config.entity.copy(scale = 2.0))

        assertEquals(4.0, HologramGeometry.baseHeight(scaled), 1e-9)
        assertEquals(4.6, HologramGeometry.topHeight(scaled, 2), 1e-9)
    }
}
