package app.simplecloud.npc.shared.hologram.placeholder

import app.simplecloud.controller.shared.group.Group
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

data class HologramPlaceholder(
    val placeholder: String,
    private val function: FunctionPlaceholder
) {

    suspend fun execute(group: Group): TagResolver {
        return Placeholder.unparsed(this.placeholder, invoke(group))
    }

    private suspend fun invoke(group: Group): String {
        return this.function.execute(group)
    }

    fun interface FunctionPlaceholder {
        suspend fun execute(group: Group): String
    }

}