package app.simplecloud.npc.shared.event.action

import app.simplecloud.npc.shared.event.EventActionHandler
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.utils.Debouncer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Niklas Nieberler
 */

class SpawnNpcActionHandler : EventActionHandler {

    private val debouncer = Debouncer(500L)

    override fun handle(namespace: NpcNamespace, id: String) {
        val npcConfig = namespace.npcRepository.get(id)
            ?: throw NullPointerException("failed to find npc $id")
        CoroutineScope(Dispatchers.IO).launch {
            debouncer.debounce(this) { namespace.hologramManager.updateHolograms(npcConfig) }
        }
    }

}