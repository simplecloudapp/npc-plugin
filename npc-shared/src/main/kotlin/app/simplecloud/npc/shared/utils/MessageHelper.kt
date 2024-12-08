package app.simplecloud.npc.shared.utils

import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.option.DefaultOptions
import app.simplecloud.npc.shared.option.OptionProvider
import org.bukkit.Bukkit

/**
 * @author Niklas Nieberler
 */

object MessageHelper {

    fun printOptionNotFoundMessage(action: Action, actionOption: Pair<String, String>, optionProvider: OptionProvider) {
        val id = optionProvider.getOption(DefaultOptions.NPC_ID)
        Bukkit.getLogger().warning("[SimpleCloud-NPC] No option was found for the Action $action for the NPC $id. Please create the option \"${actionOption.first}\"")
    }

}