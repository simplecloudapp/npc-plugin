package app.simplecloud.npc.shared.action

import app.simplecloud.npc.shared.option.OptionProvider
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

interface ActionHandler {

    fun handle(player: Player, optionProvider: OptionProvider)

}