package app.simplecloud.npc.shared.hologram.placeholder.arguments

import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue

/**
 * @author Niklas Nieberler
 */

interface ArgumentsResolver {

    suspend fun resolve(arguments: ArgumentQueue): Tag?

    fun placeholder(): String

}