package app.simplecloud.npc.shared.player

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.player.handler.*

/**
 * @author Niklas Nieberler
 */

object PlayerActions {

    private val actions = listOf(
        SendMessagePlayerActionHandler(),
        SendActionbarPlayerActionHandler(),
        SendTitlePlayerActionHandler(),
        PlaySoundPlayerActionHandler(),
//        PlayLabyEmotePlayerActionHandler()
    )

    /**
     * Returns a list with all available [PlayerActionHandler]
     * @param npcInteraction of the npc action
     */
    fun findPossibleActions(npcInteraction: NpcConfig.NpcInteraction): List<PlayerActionHandler> {
        return this.actions.filter {
            it.getOptions().any { (key, _) -> npcInteraction.getOption(key) != null }
        }
    }

    /**
     * Gets you the suggestion value for a key
     * @param key of the suggestion key
     */
    fun getSuggestionActionByKey(key: String): List<String> {
        return this.actions.map { it.getSuggestions() }
            .map { it[key] ?: listOf() }
            .flatten()
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