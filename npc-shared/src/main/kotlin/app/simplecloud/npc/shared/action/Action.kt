package app.simplecloud.npc.shared.action

import app.simplecloud.npc.shared.action.handler.*

/**
 * @author Niklas Nieberler
 */

enum class Action(
    val actionHandler: ActionHandler
) {

    OPEN_INVENTORY(OpenInventoryActionHandler()),

    RUN_COMMAND(RunCommandActionHandler()),

    RUN_CONSOLE_COMMAND(RunConsoleCommandActionHandler()),

    CONNECT_TO_SERVER(ConnectToServerActionHandler()),

    TRANSFER_TO_SERVER(TransferToServerActionHandler()),

    QUICK_JOIN(OpenInventoryActionHandler());

    companion object {
        fun getOrNull(name: String): Action? {
            return entries.firstOrNull { it.name.equals(name, true) }
        }
    }

}