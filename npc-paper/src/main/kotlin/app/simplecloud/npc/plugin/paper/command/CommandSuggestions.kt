package app.simplecloud.npc.plugin.paper.command

import app.simplecloud.npc.shared.namespace.NpcNamespace
import org.incendo.cloud.annotations.suggestion.Suggestions

/**
 * @author Niklas Nieberler
 */

open class CommandSuggestions(
    private val namespace: NpcNamespace
) {

    @Suggestions("npcIds")
    fun suggestNpcId(): List<String> {
        return this.namespace.findAllNpcs()
            .filter { this.namespace.npcManager.exist(it) }
    }

    @Suggestions("playerInteractions")
    fun suggestPlayerInteraction(): List<String> {
        return this.namespace.getAvailablePlayerInteractions().map { it.name.lowercase() }
    }

}