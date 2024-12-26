package app.simplecloud.npc.shared.inventory.placeholder

import app.simplecloud.controller.shared.server.Server
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

data class InventoryPlaceholder(
    val placeholder: String,
    private val function: FunctionPlaceholder
) {

    fun execute(server: Server): TagResolver {
        return Placeholder.unparsed(this.placeholder, invoke(server))
    }

    private fun invoke(server: Server): String {
        return this.function.execute(server)
    }

    fun interface FunctionPlaceholder {
        fun execute(server: Server): String
    }

}