package app.simplecloud.npc.provider.znpcsplus

import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.interact.ProviderInteractListener
import app.simplecloud.npc.core.interaction.PlayerInteraction
import lol.pyr.znpcsplus.api.event.NpcInteractEvent
import lol.pyr.znpcsplus.api.interaction.InteractionType
import org.bukkit.event.EventHandler

class ZNpcsPlusInteractListener(
    configIdForEventKey: (entryId: String) -> String?,
    onInteract: OnNpcInteract,
) : ProviderInteractListener(configIdForEventKey, onInteract) {

    @EventHandler
    fun handleInteract(event: NpcInteractEvent) {
        val interaction = when (event.clickType) {
            InteractionType.RIGHT_CLICK -> PlayerInteraction.RIGHT_CLICK
            InteractionType.LEFT_CLICK -> PlayerInteraction.LEFT_CLICK
            else -> return
        }

        dispatch(event.entry.id, event.player, interaction)
    }
}
