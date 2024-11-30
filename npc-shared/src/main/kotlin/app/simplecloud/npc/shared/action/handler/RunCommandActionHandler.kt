package app.simplecloud.npc.shared.action.handler

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.ActionHandler
import app.simplecloud.npc.shared.action.ActionOptions
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.utils.MessageHelper
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class RunCommandActionHandler : ActionHandler {

    private val placeholders = hashMapOf<String, (Player) -> Any>(
        "<playername>" to { it.name },
        "<playeruuid>" to { it.uniqueId }
    )

    override fun handle(player: Player, optionProvider: OptionProvider) {
        val action = ActionOptions.EXECUTE_COMMAND_NAME
        val commandOption = optionProvider.getOption(action)
        if (commandOption.isBlank()) {
            MessageHelper.printOptionNotFoundMessage(Action.RUN_COMMAND, action, optionProvider)
            return
        }

        val resolvedCommand = this.placeholders.entries.fold(commandOption) { it, entry ->
            it.replace(entry.key, entry.value(player).toString())
        }
        player.performCommand(resolvedCommand)
        MessageHelper.executeMessageFromOption(player, optionProvider)
    }

}