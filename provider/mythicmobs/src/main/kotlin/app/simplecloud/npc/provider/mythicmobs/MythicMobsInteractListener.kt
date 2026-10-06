package app.simplecloud.npc.provider.mythicmobs

import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.interact.ProviderInteractListener
import app.simplecloud.npc.core.interaction.PlayerInteraction
import io.lumine.mythic.bukkit.events.MythicMobInteractEvent
import org.bukkit.event.EventHandler

class MythicMobsInteractListener(
    configIdForEventKey: (mobReference: String) -> String?,
    onInteract: OnNpcInteract,
) : ProviderInteractListener(configIdForEventKey, onInteract) {

    @EventHandler
    fun handleInteract(event: MythicMobInteractEvent) =
        dispatch(MobReference.of(event.activeMob), event.player, PlayerInteraction.RIGHT_CLICK)
}
