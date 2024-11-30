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

    CONNECT_TO_SERVER(ConnectToServerActionHandler()),

    QUICK_JOIN(OpenInventoryActionHandler());

}