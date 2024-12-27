package app.simplecloud.npc.shared.action.handler

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.npc.shared.action.ActionHandler
import app.simplecloud.npc.shared.action.ActionOptions
import app.simplecloud.npc.shared.controller.ControllerService
import app.simplecloud.npc.shared.enums.QuantityType
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.utils.PlayerConnectionHelper
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
        CoroutineScope(Dispatchers.IO).launch {
            val server = getQuickJoinServer(optionProvider) ?: return@launch
            PlayerConnectionHelper.sendPlayerToServer(player, server)
            player.sendMessage("quickjoin server: $server")
        }
    }

    private suspend fun getQuickJoinServer(optionProvider: OptionProvider): Server? {
        val groupName = optionProvider.getOption(ActionOptions.GROUP_NAME)
        val serverState = optionProvider.getOption(ActionOptions.QUICK_JOIN_FILTER_STATE)
        val quantityType = optionProvider.getOption(ActionOptions.QUICK_JOIN_FILTER_PLAYERS)

        if (!QuantityType.exist(quantityType)) {
            Bukkit.getLogger().warning("[SimpleCloud-NPC] No possible quantity type was found in filter.server.state! Please use ${QuantityType.entries.joinToString(", ") { it.name.lowercase() }}")
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


    override fun getOptions() = listOf(
        ActionOptions.GROUP_NAME,
        ActionOptions.QUICK_JOIN_FILTER_STATE,
        ActionOptions.QUICK_JOIN_FILTER_PLAYERS,
    )
}