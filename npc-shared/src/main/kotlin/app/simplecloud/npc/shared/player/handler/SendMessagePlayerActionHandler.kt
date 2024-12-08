package app.simplecloud.npc.shared.player.handler

import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.player.PlayerActionHandler
import app.simplecloud.npc.shared.player.PlayerActionOptions
import app.simplecloud.npc.shared.text
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class SendMessagePlayerActionHandler : PlayerActionHandler {

    override fun handle(player: Player, optionProvider: OptionProvider) {
        val message = optionProvider.getOption(PlayerActionOptions.SEND_MESSAGE)
        player.sendMessage(text(message))
    }

    override fun getOptions() = listOf(
        PlayerActionOptions.SEND_MESSAGE
    )

}