package app.simplecloud.npc.shared.action

import app.simplecloud.npc.shared.enums.QuantityType
import build.buf.gen.simplecloud.controller.v1.ServerState

/**
 * @author Niklas Nieberler
 */

object ActionOptions {

    val EXECUTE_COMMAND_NAME = Pair("command.name", "")

    val CONNECT_TO_SERVER_NAME = Pair("server.name", "")

    val OPEN_INVENTORY = Pair("inventory.name", "")

    val TRANSFER_SERVER_IP = Pair("server.ip", "")

    val TRANSFER_SERVER_PORT = Pair("server.port", "25565")

    val GROUP_NAME = Pair("group.name", "")

    val QUICK_JOIN_FILTER_PLAYERS = Pair("filter.player.count", QuantityType.LEAST)

    val QUICK_JOIN_FILTER_STATE = Pair("filter.server.state", ServerState.AVAILABLE)

    val QUICK_JOIN_FILTER_PROPERTY = Pair("filter.server.property", "")

}