package app.simplecloud.npc.shared.action.interaction

import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.option.OptionProvider
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class InteractionExecutor(
    private val namespace: NpcNamespace
) {

    /**
     * Executes the appropriate action from the [PlayerInteraction] for this npc
     * @param id of the npc
     * @param player the action executor
     * @param playerInteraction the action type
     * @param optionProvider the possible options
     */
    fun execute(id: String, player: Player, playerInteraction: PlayerInteraction, optionProvider: OptionProvider = OptionProvider()) {
        val config = this.namespace.npcRepository.get(id) ?: return
        val interaction = config.getPlayerInteraction(playerInteraction) ?: return
        optionProvider.add(
            *interaction.options.map { Option.of(it) }.toTypedArray(),
            *config.options.map { Option.of(it) }.toTypedArray()
        )
        interaction.action.actionHandler.handle(player, this.namespace, optionProvider)
    }

}