package app.simplecloud.npc.core.config

import app.simplecloud.npc.core.interaction.PlayerInteraction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ActionFieldsTest {

    @Test
    fun `an empty join state action falls back to the public one`() {
        val config = NpcConfig(
            actions = mutableListOf(
                NpcConfig.ActionConfiguration(PlayerInteraction.LEFT_CLICK, sendMessage = "hi"),
                NpcConfig.ActionConfiguration(PlayerInteraction.LEFT_CLICK, joinState = "offline"),
            ),
        )

        assertEquals("hi", config.actionsFor(PlayerInteraction.LEFT_CLICK, "offline").single().sendMessage)
    }

    @Test
    fun `an action holding only a permission or cooldown is kept`() {
        assertFalse(NpcConfig.ActionConfiguration(permission = "vip").isEmpty())
        assertFalse(NpcConfig.ActionConfiguration(cooldown = 3_000).isEmpty())
        assertTrue(NpcConfig.ActionConfiguration().isEmpty())
    }
}
