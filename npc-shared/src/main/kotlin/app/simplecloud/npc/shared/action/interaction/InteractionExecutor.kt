package app.simplecloud.npc.shared.action.interaction

import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.repository.NpcRepository
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class InteractionExecutor(
    private val repository: NpcRepository
) {

    /**
     *
     */
    fun execute(id: String, player: Player, playerInteraction: PlayerInteraction, optionProvider: OptionProvider = OptionProvider()) {
        val config = this.repository.get(id) ?: return
        val interaction = config.getPlayerInteraction(playerInteraction) ?: return
        optionProvider.add(
            *interaction.options.map { Option.of(it) }.toTypedArray(),
            *config.options.map { Option.of(it) }.toTypedArray()
        )
        interaction.action.actionHandler.handle(player, optionProvider)
    }

}