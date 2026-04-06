package app.simplecloud.npc.shared.action.interaction

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.player.PlayerActions
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
        val config = this.namespace.npcRepository.find(id) ?: return
        val sneakingInteraction = PlayerInteraction.getOrNull("SHIFT_$playerInteraction")
        if (player.isSneaking && sneakingInteraction != null && config.existPlayerInteraction(sneakingInteraction)) {
            executeSingle(config, player, sneakingInteraction, optionProvider)
            return
        }
        executeSingle(config, player, playerInteraction, optionProvider)
    }

    private fun executeSingle(config: NpcConfig, player: Player, playerInteraction: PlayerInteraction, optionProvider: OptionProvider) {
        val interaction = config.getPlayerInteraction(playerInteraction) ?: return
        optionProvider.add(
            *interaction.getOptions().toTypedArray(),
            *config.options.map { Option(it.key, it.value) }.toTypedArray()
        )
        interaction.action.actionHandler.handle(player, this.namespace, optionProvider)
        PlayerActions.findPossibleActions(interaction).forEach {
            it.handle(player, optionProvider)
        }
    }

}