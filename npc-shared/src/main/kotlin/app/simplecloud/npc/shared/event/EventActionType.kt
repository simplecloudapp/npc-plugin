package app.simplecloud.npc.shared.event

import app.simplecloud.npc.shared.event.action.*

/**
 * @author Niklas Nieberler
 */

enum class EventActionType(
    val eventActionHandler: EventActionHandler
) {

    CREATE(CreateNpcActionHandler()),

    SPAWN(SpawnNpcActionHandler()),

    REMOVE(RemoveNpcActionHandler()),

}
