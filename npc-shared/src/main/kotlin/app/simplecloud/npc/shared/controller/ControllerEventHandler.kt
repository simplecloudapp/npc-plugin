package app.simplecloud.npc.shared.controller

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.npc.shared.hologram.JoinStateHelper
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.utils.Debouncer
import build.buf.gen.simplecloud.controller.v1.ServerUpdateEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Niklas Nieberler
 */

class ControllerEventHandler(
    private val controllerApi: ControllerApi.Coroutine
) {

    private val debouncer = Debouncer(1000L)

    fun registerEvents(namespace: NpcNamespace) {
        val pubSubClient = this.controllerApi.getPubSubClient()
        pubSubClient.subscribe("event", ServerUpdateEvent::class.java) { event ->
            CoroutineScope(Dispatchers.IO).launch {
                debouncer.debounce(this) { updateHolograms(namespace) }
            }
        }
    }

    private suspend fun updateHolograms(namespace: NpcNamespace) {
        namespace.findAllNpcs().forEach {
            val config = namespace.npcRepository.get(it)
                ?: throw NullPointerException("failed to find npc $it")
            val joinState = JoinStateHelper.getJoinState(config)
            namespace.hologramManager.updateTextHologram(config, joinState)
        }
    }

}