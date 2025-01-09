package app.simplecloud.npc.namespace.mythicmobs.listener

import app.simplecloud.npc.namespace.mythicmobs.MobIdFetcher
import app.simplecloud.npc.namespace.mythicmobs.MythicMobsNamespace
import app.simplecloud.npc.namespace.mythicmobs.option.MythicMobsOptionProviders
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import io.lumine.mythic.bukkit.events.MythicMobInteractEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * @author Niklas Nieberler
 */

class MythicMobInteractListener(
    private val namespace: MythicMobsNamespace
) : Listener {

    @EventHandler
    fun handleInteract(event: MythicMobInteractEvent) {
        val player = event.player
        val mob = event.activeMob
        val mobId = MobIdFetcher.fetch(mob)

        val interactionExecutor = this.namespace.interactionExecutor
        val optionProvider = MythicMobsOptionProviders.createInteractOptionProviders(mob)
        interactionExecutor.execute(mobId, player, PlayerInteraction.RIGHT_CLICK, optionProvider)
    }

}