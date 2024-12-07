package app.simplecloud.npc.shared.event.action

import app.simplecloud.npc.shared.event.EventActionHandler
import app.simplecloud.npc.shared.namespace.NpcNamespace

/**
 * @author Niklas Nieberler
 */

class RemoveNpcActionHandler : EventActionHandler {

    override fun handle(namespace: NpcNamespace, id: String) {
        namespace.npcManager.delete(id)
    }

}