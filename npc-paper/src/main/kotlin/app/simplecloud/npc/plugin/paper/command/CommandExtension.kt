package app.simplecloud.npc.plugin.paper.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * @author Niklas Nieberler
 */

private val miniMessage = MiniMessage.miniMessage()

const val commandName = "simplecloudnpc|scnpc|cloudnpc"

const val PREFIX = "<#38bdf8><bold>⚡</bold>"

fun text(message: String): Component {
    return miniMessage.deserialize(message)
}