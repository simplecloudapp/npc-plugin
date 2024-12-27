package app.simplecloud.npc.shared.inventory.placeholder

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.npc.shared.decodeComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

class InventoryPlaceholderHandler {

    private val defaultPlaceholders = listOf(
        InventoryPlaceholder("group_name") { it.group },
        InventoryPlaceholder("player_count") { it.playerCount.toString() },
        InventoryPlaceholder("max_players") { it.maxPlayers.toString() },
        InventoryPlaceholder("min_memory") { it.minMemory.toString() },
        InventoryPlaceholder("max_memory") { it.maxMemory.toString() },
        InventoryPlaceholder("state") { it.state.toString() },
        InventoryPlaceholder("type") { it.type.toString() },
        InventoryPlaceholder("numerical_id") { it.numericalId.toString() },
        InventoryPlaceholder("unique_id") { it.uniqueId.toString() },
        InventoryPlaceholder("port") { it.port.toString() },
        InventoryPlaceholder("ip") { it.ip },
    )

    private val miniMessage = MiniMessage.miniMessage()

    fun placeholderComponent(server: Server, text: String): Component {
        return decodeComponent(this.miniMessage.deserialize(
            text,
            *this.defaultPlaceholders.map { it.execute(server) }.toTypedArray(),
            getPropertyTagResolver(server)
        ))
    }

    private fun getPropertyTagResolver(server: Server): TagResolver {
        return TagResolver.resolver("property") { arguments, _ ->
            val argumentName = arguments.popOr("placeholder").value()
            val string = server.properties[argumentName] ?: "empty"
            Tag.preProcessParsed(string)
        }
    }

}