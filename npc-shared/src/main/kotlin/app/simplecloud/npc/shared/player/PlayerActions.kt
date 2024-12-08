package app.simplecloud.npc.shared.player

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.player.handler.*

/**
 * @author Niklas Nieberler
 */

object PlayerActions {

    private val actions = listOf(
        SendMessagePlayerActionHandler(),
        SendActionbarPlayerActionHandler()
    )

    /**
     * Returns a list with all available [PlayerActionHandler]
     * @param npcInteraction of the npc action
     */
    fun findPossibleActions(npcInteraction: NpcConfig.NpcInteraction): List<PlayerActionHandler> {
        return actions.filter {
            it.getOptions().any { (key, _) -> npcInteraction.getOption(key) != null }
        }
    }

    /**
     * Returns list of all option keys
     */
    fun getAllOptionKeys(): List<String> {
        return this.actions
            .map { it.getOptions() }
            .flatten()
            .map { it.first }
    }

}