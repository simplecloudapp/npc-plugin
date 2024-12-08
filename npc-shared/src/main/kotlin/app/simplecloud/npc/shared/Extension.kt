package app.simplecloud.npc.shared

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * @author Niklas Nieberler
 */

private val miniMessage = MiniMessage.miniMessage()

fun text(message: String): Component {
    return miniMessage.deserialize(message)
}