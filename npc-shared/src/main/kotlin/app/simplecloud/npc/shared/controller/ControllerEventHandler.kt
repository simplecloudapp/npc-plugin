package app.simplecloud.npc.shared.controller

import app.simplecloud.api.CloudApi
import app.simplecloud.npc.shared.hologram.JoinStateHelper
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.utils.Debouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Niklas Nieberler
 */

class ControllerEventHandler(
    private val cloudApi: CloudApi,
) {

    private val debouncer = Debouncer(1000L)

    fun registerEvents(namespace: NpcNamespace) {
        val event = this.cloudApi.event()
        event.server().onUpdated { registerEvent(namespace) }
        event.group().onUpdated { registerEvent(namespace) }
    }

    private fun registerEvent(namespace: NpcNamespace) {
        CoroutineScope(Dispatchers.IO).launch {
            debouncer.debounce(this) { updateHolograms(namespace) }
        }
    }

    private suspend fun updateHolograms(namespace: NpcNamespace) {
        namespace.findAllNpcs()
            .filter { namespace.npcManager.exist(it) }
            .forEach {
                val config = namespace.npcRepository.get(it)
                    ?: throw NullPointerException("failed to find npc $it")
                val joinState = JoinStateHelper.getJoinState(config)
                namespace.hologramManager.updateTextHologram(config, joinState)
            }
    }

}