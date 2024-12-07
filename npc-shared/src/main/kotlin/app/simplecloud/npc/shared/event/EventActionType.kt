package app.simplecloud.npc.shared.event

import app.simplecloud.npc.shared.event.action.RemoveNpcActionHandler
import app.simplecloud.npc.shared.event.action.SpawnNpcActionHandler

/**
 * @author Niklas Nieberler
 */

enum class EventActionType(
    val eventActionHandler: EventActionHandler
) {

    SPAWN(SpawnNpcActionHandler()),

    REMOVE(RemoveNpcActionHandler()),

}
