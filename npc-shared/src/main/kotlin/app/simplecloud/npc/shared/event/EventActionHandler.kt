package app.simplecloud.npc.shared.event

import app.simplecloud.npc.shared.namespace.NpcNamespace

/**
 * @author Niklas Nieberler
 */

fun interface EventActionHandler {

    /**
     * Executes this method when an action is called
     * @param namespace of the npc plugin
     * @param id of the npc
     */
    fun handle(namespace: NpcNamespace, id: String)

}