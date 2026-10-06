package app.simplecloud.npc.provider.fancynpcs

import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.interact.ProviderInteractListener
import app.simplecloud.npc.core.interaction.PlayerInteraction
import de.oliver.fancynpcs.api.actions.ActionTrigger
import de.oliver.fancynpcs.api.events.NpcInteractEvent
import org.bukkit.event.EventHandler

class FancyNpcsInteractListener(
    configIdForEventKey: (npcDataId: String) -> String?,
    onInteract: OnNpcInteract,
) : ProviderInteractListener(configIdForEventKey, onInteract) {

    @EventHandler
    fun handleInteract(event: NpcInteractEvent) {
        val interaction = when (event.interactionType) {
            ActionTrigger.LEFT_CLICK -> PlayerInteraction.LEFT_CLICK
            ActionTrigger.RIGHT_CLICK -> PlayerInteraction.RIGHT_CLICK
            else -> return
        }

        dispatch(event.npc.data.id, event.player, interaction)
    }
}
