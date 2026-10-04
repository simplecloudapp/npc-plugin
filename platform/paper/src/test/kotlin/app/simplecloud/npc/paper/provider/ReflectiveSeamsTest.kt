package app.simplecloud.npc.paper.provider

import app.simplecloud.npc.core.render.NpcRenderer
import org.bukkit.plugin.Plugin
import kotlin.test.Test
import kotlin.test.assertTrue

class ReflectiveSeamsTest {

    @Test
    fun `mannequin renderer exists with a plugin constructor`() {
        val clazz = Class.forName(ReflectiveSeams.MANNEQUIN_RENDERER)
        clazz.getDeclaredConstructor(Plugin::class.java)
        assertTrue(NpcRenderer::class.java.isAssignableFrom(clazz))
    }
}
