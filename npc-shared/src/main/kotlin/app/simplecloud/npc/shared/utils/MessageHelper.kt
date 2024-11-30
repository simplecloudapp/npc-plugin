package app.simplecloud.npc.shared.utils

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.ActionOptions
import app.simplecloud.npc.shared.option.DefaultOptions
import app.simplecloud.npc.shared.option.OptionProvider
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

object MessageHelper {

    private val miniMessage = MiniMessage.miniMessage()

    fun printOptionNotFoundMessage(action: Action, actionOption: Pair<String, String>, optionProvider: OptionProvider) {
        val id = optionProvider.getOption(DefaultOptions.NPC_ID)
        Bukkit.getLogger().warning("[SimpleCloud-NPC] No option was found for the Action $action for the NPC $id. Please create the option \"${actionOption.first}\"")
    }

    fun executeMessageFromOption(player: Player, optionProvider: OptionProvider) {
        val messageOption = optionProvider.getOption(ActionOptions.SEND_MESSAGE)
        if (messageOption.isBlank())
            return
        player.sendMessage(this.miniMessage.deserialize(messageOption))
    }

}