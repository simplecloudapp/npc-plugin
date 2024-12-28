package app.simplecloud.npc.shared.action.handler

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.ActionHandler
import app.simplecloud.npc.shared.action.ActionOptions
import app.simplecloud.npc.shared.controller.ControllerService
import app.simplecloud.npc.shared.enums.QuantityType
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.utils.MessageHelper
import app.simplecloud.npc.shared.utils.PlayerConnectionHelper
import build.buf.gen.simplecloud.controller.v1.ServerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * @author Niklas Nieberler
 */

class QuickJoinActionHandler : ActionHandler {

    override fun handle(player: Player, namespace: NpcNamespace, optionProvider: OptionProvider) {
        if (!optionProvider.hasOption(ActionOptions.GROUP_NAME)) {
            MessageHelper.printOptionNotFoundMessage(Action.QUICK_JOIN, ActionOptions.GROUP_NAME, optionProvider)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val server = getQuickJoinServer(optionProvider)
            if (server == null) {
                sendFailedServerFinderMessage(player, optionProvider)
                return@launch
            }

            val serverNamePattern = optionProvider.getOption(ActionOptions.SERVER_NAME_PATTERN)
                .replace("<group_name>", server.group)
                .replace("<numerical_id>", server.numericalId.toString())

            PlayerConnectionHelper.sendPlayerToServer(player, serverNamePattern)
        }
    }

    private suspend fun getQuickJoinServer(optionProvider: OptionProvider): Server? {
        val groupName = optionProvider.getOption(ActionOptions.GROUP_NAME)
        val serverState = optionProvider.getOption(ActionOptions.QUICK_JOIN_FILTER_STATE)
        val quantityType = optionProvider.getOption(ActionOptions.QUICK_JOIN_FILTER_PLAYERS)

        val logger = Bukkit.getLogger()
        if (!QuantityType.exist(quantityType)) {
            logger.warning("[SimpleCloud-NPC] No possible quantity type was found in filter.player.state! Please use ${QuantityType.entries.joinToString(", ") { it.name.lowercase() }}")
            return null
        }

        if (!ServerState.entries.any { it.name.equals(serverState, ignoreCase = true) }) {
            logger.warning("[SimpleCloud-NPC] No possible server state was found in filter.server.state! Please use ${ServerState.entries.joinToString(", ") { it.name.lowercase() }}")
            return null
        }

        val servers = ControllerService.controllerApi.getServers().getServersByGroup(groupName)
            .filter { serverState.equals(it.state.name, ignoreCase = true) }
        return when (QuantityType.valueOf(quantityType.uppercase())) {
            QuantityType.MOST -> servers.maxByOrNull { it.playerCount }
            QuantityType.LEAST -> servers.minByOrNull { it.playerCount }
            else -> servers.firstOrNull()
        }
    }

    private fun sendFailedServerFinderMessage(player: Player, optionProvider: OptionProvider) {
        val serverFinderMessage = ActionOptions.FAILED_SERVER_FINDER_MESSAGE
        if (optionProvider.hasOption(serverFinderMessage)) {
            player.sendMessage(optionProvider.getOption(serverFinderMessage))
        }
    }

    override fun getOptions() = listOf(
        ActionOptions.GROUP_NAME,
        ActionOptions.FAILED_SERVER_FINDER_MESSAGE,
        ActionOptions.SERVER_NAME_PATTERN,
        ActionOptions.QUICK_JOIN_FILTER_STATE,
        ActionOptions.QUICK_JOIN_FILTER_PLAYERS,
    )
}