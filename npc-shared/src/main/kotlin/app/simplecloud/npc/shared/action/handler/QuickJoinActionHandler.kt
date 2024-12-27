package app.simplecloud.npc.shared.action.handler

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.ActionHandler
import app.simplecloud.npc.shared.action.ActionOptions
import app.simplecloud.npc.shared.controller.ControllerService
import app.simplecloud.npc.shared.enums.QuantityType
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.option.OptionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class QuickJoinActionHandler : ActionHandler {

    override fun handle(player: Player, namespace: NpcNamespace, optionProvider: OptionProvider) {
        CoroutineScope(Dispatchers.IO).launch {
            val server = getQuickJoinServer(optionProvider)
            player.sendMessage("quickjoin server: $server")
        }
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private suspend fun getQuickJoinServer(optionProvider: OptionProvider): Server? {
        val groupName = optionProvider.getOption(ActionOptions.GROUP_NAME)
        val serverStates = optionProvider.getOptions(ActionOptions.QUICK_JOIN_FILTER_STATE)

        serverStates.forEach {
            println("state: $it")
        }

        val servers = ControllerService.controllerApi.getServers().getServersByGroup(groupName)

        println("servers: ${servers.size}")

        val filteredServers = if (optionProvider.hasOption(ActionOptions.QUICK_JOIN_FILTER_PROPERTY)) {
            val requiredProperties = optionProvider.getOptions(ActionOptions.QUICK_JOIN_FILTER_PROPERTY)
            servers.filter { server -> server.properties.any { it as? String in requiredProperties } }
        } else {
            servers
        }

        println("servers2: ${servers.size}")

        val quantityType = optionProvider.getOption(ActionOptions.QUICK_JOIN_FILTER_PLAYERS)
        return filteredServers
            .sortedWith(compareBy({ it.playerCount }, { quantityType == QuantityType.MOST }))
            .firstOrNull()
    }

    override fun getOptions() = listOf(
        ActionOptions.GROUP_NAME,
        ActionOptions.QUICK_JOIN_FILTER_STATE,
        ActionOptions.QUICK_JOIN_FILTER_PROPERTY,
        ActionOptions.QUICK_JOIN_FILTER_STATE,
    )
}