package app.simplecloud.npc.namespace.citizens.listener

import app.simplecloud.npc.namespace.citizens.option.CitizensOptionProviders
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.provider.NpcProviderEvents
import app.simplecloud.npc.shared.provider.NpcProviderType
import net.citizensnpcs.api.event.NPCRightClickEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class NpcRightClickListener(
    private val events: NpcProviderEvents,
) : Listener {

    @EventHandler
    fun handleNpcRightClick(event: NPCRightClickEvent) {
        events.interact(
            NpcProviderType.CITIZENS,
            event.npc.id.toString(),
            event.clicker,
            PlayerInteraction.RIGHT_CLICK,
            CitizensOptionProviders.createInteractOptionProviders(event.npc),
        )
    }
}
