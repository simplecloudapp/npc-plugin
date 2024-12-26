package app.simplecloud.npc.shared.hologram.placeholder.arguments

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.group.Group
import build.buf.gen.simplecloud.controller.v1.ServerState
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue

/**
 * @author Niklas Nieberler
 */

class ServerCountResolver(
    private val group: Group?,
    private val controllerApi: ControllerApi.Coroutine
) : ArgumentsResolver {

    override suspend fun resolve(arguments: ArgumentQueue): Tag? {
        if (this.group == null)
            return null
        val text = arguments.popOr("all").value()
        val serverState = ServerState.entries.firstOrNull { it.name.lowercase() == text }
        return Tag.preProcessParsed(findServerCount(this.group, serverState).toString())
    }

    override fun placeholder() = "server_count"

    private suspend fun findServerCount(group: Group, state: ServerState?): Int {
        return this.controllerApi.getServers().getServersByGroup(group)
            .filter { it.state == state }
            .size
    }

}