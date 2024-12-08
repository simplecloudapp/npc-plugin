package app.simplecloud.npc.plugin.paper.command

import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.text
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

abstract class AbstractNpcCommand(
    val namespace: NpcNamespace
) : CommandSuggestions(
    namespace
) {

    fun findPlayerInteraction(player: Player, playerInteraction: String): PlayerInteraction? {
        val interaction = PlayerInteraction.getOrNull(playerInteraction)
        if (interaction == null) {
            player.sendMessage(text("$PREFIX <#dc2626>This type does not exist! Please use ${PlayerInteraction.entries.joinToString(", ") { it.name.lowercase() }}"))
            return null
        }
        return interaction
    }

    fun findNpcConfigById(player: Player, id: String): NpcConfig? {
        val npcConfig = this.namespace.npcRepository.get(id)
        if (npcConfig == null) {
            player.sendMessage(text("$PREFIX <#dc2626>Npc with id $id does not exist!"))
            return null
        }
        return npcConfig
    }

}