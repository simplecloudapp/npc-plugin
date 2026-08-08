package app.simplecloud.npc.namespace.mythicmobs.listener

import app.simplecloud.npc.namespace.mythicmobs.MobIdFetcher
import app.simplecloud.npc.namespace.mythicmobs.option.MythicMobsOptionProviders
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.provider.NpcProviderEvents
import app.simplecloud.npc.shared.provider.NpcProviderType
import io.lumine.mythic.bukkit.events.MythicMobInteractEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class MythicMobInteractListener(
    private val events: NpcProviderEvents,
) : Listener {

    @EventHandler
    fun handleInteract(event: MythicMobInteractEvent) {
        events.interact(
            NpcProviderType.MYTHIC_MOBS,
            MobIdFetcher.fetch(event.activeMob),
            event.player,
            PlayerInteraction.RIGHT_CLICK,
            MythicMobsOptionProviders.createInteractOptionProviders(event.activeMob),
        )
    }
}
