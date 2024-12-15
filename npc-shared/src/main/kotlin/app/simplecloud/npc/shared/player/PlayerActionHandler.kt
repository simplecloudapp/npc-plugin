package app.simplecloud.npc.shared.player

import app.simplecloud.npc.shared.option.OptionProvider
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

interface PlayerActionHandler {

    fun handle(player: Player, optionProvider: OptionProvider)

    fun getOptions(): List<Pair<String, Any>>

    fun getSuggestions(): Map<String, List<String>> = emptyMap()

}