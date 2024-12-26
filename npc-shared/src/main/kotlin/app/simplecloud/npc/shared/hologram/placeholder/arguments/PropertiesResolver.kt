package app.simplecloud.npc.shared.hologram.placeholder.arguments

import app.simplecloud.controller.shared.group.Group
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue

/**
 * @author Niklas Nieberler
 */

class PropertiesResolver(
    private val group: Group?
) : ArgumentsResolver {

    override suspend fun resolve(arguments: ArgumentQueue): Tag? {
        val argumentName = arguments.popOr("placeholder").value()
        val string = this.group?.properties[argumentName] ?: "empty"
        return Tag.preProcessParsed(string)
    }

    override fun placeholder() = "property"

}