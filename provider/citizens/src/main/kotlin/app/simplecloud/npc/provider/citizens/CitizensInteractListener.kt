package app.simplecloud.npc.provider.citizens

import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.interact.ProviderInteractListener
import app.simplecloud.npc.core.interaction.PlayerInteraction
import net.citizensnpcs.api.event.NPCLeftClickEvent
import net.citizensnpcs.api.event.NPCRightClickEvent
import org.bukkit.event.EventHandler

class CitizensInteractListener(
    configIdForEventKey: (npcUuid: String) -> String?,
    onInteract: OnNpcInteract,
) : ProviderInteractListener(configIdForEventKey, onInteract) {

    @EventHandler
    fun handleLeftClick(event: NPCLeftClickEvent) =
        dispatch(event.npc.uniqueId.toString(), event.clicker, PlayerInteraction.LEFT_CLICK)

    @EventHandler
    fun handleRightClick(event: NPCRightClickEvent) =
        dispatch(event.npc.uniqueId.toString(), event.clicker, PlayerInteraction.RIGHT_CLICK)
}
