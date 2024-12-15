package app.simplecloud.npc.shared.player.handler

import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.player.PlayerActionHandler
import app.simplecloud.npc.shared.player.PlayerActionOptions
import app.simplecloud.npc.shared.text
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class SendTitlePlayerActionHandler : PlayerActionHandler {

    override fun handle(player: Player, optionProvider: OptionProvider) {
        val titleOption = optionProvider.getOption(PlayerActionOptions.SEND_TITLE)
        val subtitleOption = optionProvider.getOption(PlayerActionOptions.SEND_SUBTITLE)

        val title = Title.title(text(titleOption), text(subtitleOption))
        player.showTitle(title)
    }

    override fun getOptions() = listOf(
        PlayerActionOptions.SEND_TITLE,
        PlayerActionOptions.SEND_SUBTITLE
    )

}