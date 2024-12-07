package app.simplecloud.npc.shared.event

import app.simplecloud.npc.shared.event.listener.FancyEventListener
import app.simplecloud.npc.shared.namespace.NpcNamespace
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.plugin.Plugin

/**
 * @author Niklas Nieberler
 */

class EventManager(
    private val namespace: NpcNamespace
) {

    /**
     * Creates the npc listeners for the interaction of the npcs
     * @param event to register
     * @param javaPlugin the npc plugin
     * @param eventActionType the action
     * @param function where to get the npc id
     * @param condition for the event
     */
    fun <E : Event> registerActionEvent(
        event: Class<E>,
        javaPlugin: Plugin,
        eventActionType: EventActionType,
        function: (E) -> String,
        condition: (E) -> Boolean = { true }
    ) {
        FancyEventListener(event, EventPriority.NORMAL, javaPlugin)
            .addCondition(condition)
            .addAction { eventActionType.eventActionHandler.handle(this.namespace, function(it)) }
    }

}

/**
 * Creates the npc listeners for the interaction of the npcs
 * @param javaPlugin the npc plugin
 * @param eventActionType the action
 * @param function where to get the npc id
 * @param condition for the event
 */
inline fun <reified E : Event> EventManager.registerActionEvent(
    javaPlugin: Plugin,
    eventActionType: EventActionType,
    noinline function: (E) -> String,
    noinline condition: (E) -> Boolean = { true }
) {
    registerActionEvent(E::class.java, javaPlugin, eventActionType, function, condition)
}