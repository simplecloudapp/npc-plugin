package app.simplecloud.npc.namespace.znpcsplus.listener

import app.simplecloud.npc.namespace.znpcsplus.option.ZNpcsPlusOptionProviders
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.provider.NpcProviderEvents
import app.simplecloud.npc.shared.provider.NpcProviderType
import lol.pyr.znpcsplus.api.event.NpcInteractEvent
import lol.pyr.znpcsplus.api.interaction.InteractionType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class NpcInteractListener(
    private val events: NpcProviderEvents,
) : Listener {

    @EventHandler
    fun handleInteract(event: NpcInteractEvent) {
        val interaction = if (event.clickType == InteractionType.RIGHT_CLICK) {
            PlayerInteraction.RIGHT_CLICK
        } else {
            PlayerInteraction.LEFT_CLICK
        }
        events.interact(
            NpcProviderType.ZNPCS_PLUS,
            event.entry.id,
            event.player,
            interaction,
            ZNpcsPlusOptionProviders.createInteractOptionProviders(event.entry),
        )
    }
}
