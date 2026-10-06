package app.simplecloud.npc.common.inventory

import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryRepository
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExampleInventoryTest {

    private fun load(): InventoryConfiguration {
        val directory = createTempDirectory("npc-example-inventory")
        val resource = assertNotNull(javaClass.getResourceAsStream("/inventories/example.yml"), "resource missing")
        directory.resolve("example.yml").writeBytes(resource.readBytes())

        return assertNotNull(InventoryRepository(directory).load().singleOrNull())
    }

    @Test
    fun `example loads with every documented slot`() {
        val config = load()
        assertEquals("example", config.id)
        assertEquals(1, config.version)
        assertEquals(5, config.rows)
        assertEquals(22, config.items.size)
        assertTrue(config.items.all { it.slot in 0 until config.size() })
        assertEquals(config.items.size, config.items.map { it.slot }.distinct().size, "no duplicate slots")

        val star = assertNotNull(config.items.firstOrNull { it.slot == 4 })
        assertTrue(star.actions.single().joinTarget, "the star joins the NPC's target")
    }
}
