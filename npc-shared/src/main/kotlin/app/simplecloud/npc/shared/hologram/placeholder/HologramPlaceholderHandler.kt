package app.simplecloud.npc.shared.hologram.placeholder

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.npc.shared.controller.ControllerService
import app.simplecloud.npc.shared.hologram.HologramOptions
import app.simplecloud.npc.shared.hologram.placeholder.arguments.*
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.text
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

class HologramPlaceholderHandler(
    private val namespace: NpcNamespace,
) {

    private val defaultPlaceholders = listOf(
        HologramPlaceholder("group_name") { it.name },
        HologramPlaceholder("max_players") { it.maxPlayers.toString() },
        HologramPlaceholder("online_players") { getOnlinePlayersByGroup(it).toString() }
    )

    private val miniMessage = MiniMessage.miniMessage()
    private val controllerApi = ControllerService.controllerApi

    fun syncPlaceholderComponent(id: String, text: String): Component {
        return runBlocking { return@runBlocking placeholderComponent(id, text) }
    }

    suspend fun placeholderComponent(id: String, text: String): Component {
        val group = getGroupByConfig(id) ?: return text("empty group lol")
        return this.miniMessage.deserialize(
            text,
            TagResolver.resolver(
                *executeTagResolver(group),
                *buildArgumentsResolvers(group)
            )
        )
    }

    private suspend fun executeTagResolver(group: Group?): Array<TagResolver> {
        if (group == null)
            return emptyArray()
        return this.defaultPlaceholders.map { it.execute(group) }.toTypedArray()
    }

    private fun buildArgumentsResolvers(group: Group?): Array<TagResolver> {
        return arrayOf(
            createArgumentResolver(PlayerCountResolver(group, this.controllerApi)),
            createArgumentResolver(ServerCountResolver(group, this.controllerApi)),
            createArgumentResolver(PropertiesResolver(group))
        )
    }

    private fun createArgumentResolver(resolver: ArgumentsResolver): TagResolver {
        return TagResolver.resolver(resolver.placeholder()) { arguments, _ ->
            return@resolver runBlocking { resolver.resolve(arguments) }
        }
    }

    private suspend fun getGroupByConfig(id: String): Group? {
        val config = this.namespace.npcRepository.get(id) ?: return null
        val groupName = config.getOptionProvider().getOption(HologramOptions.PLACEHOLDER_GROUP_NAME)
        return this.controllerApi.getGroups().getGroupByName(groupName)
    }

    private suspend fun getOnlinePlayersByGroup(group: Group): Long {
        return this.controllerApi.getServers().getServersByGroup(group)
            .sumOf { it.playerCount }
    }

}