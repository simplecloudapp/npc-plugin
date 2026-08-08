package app.simplecloud.npc.namespace.fancynpcs.listener

import app.simplecloud.npc.namespace.fancynpcs.option.FancyNpcsOptionProviders
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.provider.NpcProviderEvents
import app.simplecloud.npc.shared.provider.NpcProviderType
import de.oliver.fancynpcs.api.actions.ActionTrigger
import de.oliver.fancynpcs.api.events.NpcInteractEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class NpcInteractListener(
    private val events: NpcProviderEvents,
) : Listener {

    @EventHandler
    fun handleInteract(event: NpcInteractEvent) {
        val interaction = if (event.interactionType == ActionTrigger.RIGHT_CLICK) {
            PlayerInteraction.RIGHT_CLICK
        } else {
            PlayerInteraction.LEFT_CLICK
        }
        events.interact(
            NpcProviderType.FANCY_NPCS,
            event.npc.data.id,
            event.player,
            interaction,
            FancyNpcsOptionProviders.createInteractOptionProviders(event.npc),
        )
    }
}
