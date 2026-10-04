package app.simplecloud.npc.bukkit.interact

import app.simplecloud.npc.core.interaction.PlayerInteraction
import org.bukkit.entity.Player
import org.bukkit.event.Listener

abstract class ProviderInteractListener(
    private val configIdForEventKey: (eventKey: String) -> String?,
    private val onInteract: OnNpcInteract,
) : Listener {
    protected fun dispatch(eventKey: String, player: Player, interaction: PlayerInteraction) {
        val id = configIdForEventKey(eventKey) ?: return
        onInteract(id, player, interaction)
    }
}
