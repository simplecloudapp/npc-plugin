package app.simplecloud.npc.shared.event

import app.simplecloud.npc.shared.event.action.*

/**
 * @author Niklas Nieberler
 */

enum class EventActionType(
    val eventActionHandler: EventActionHandler
) {

    SPAWN(SpawnNpcActionHandler()),

    REMOVE(RemoveNpcActionHandler()),

}
