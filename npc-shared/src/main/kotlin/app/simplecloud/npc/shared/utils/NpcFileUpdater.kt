package app.simplecloud.npc.shared.utils

import app.simplecloud.npc.shared.config.NpcConfig
import kotlin.collections.set

/**
 * @author Niklas Nieberler
 */

object NpcFileUpdater {

    private val lastUpdateTimes = mutableMapOf<String, Long>()

    private val functions = hashMapOf<String, (NpcConfig) -> Unit>()

    fun updateFileRequest(key: String, function: (NpcConfig) -> Unit) {
        this.functions[key] = function
    }

    fun invokeFile(entry: NpcConfig) {
        val id = entry.id
        val currentTime = System.currentTimeMillis()
        val lastUpdateTime = this.lastUpdateTimes[id] ?: 0

        if (currentTime - lastUpdateTime >= 500) {
            this.lastUpdateTimes[id] = currentTime
            this.functions.forEach { it.value(entry) }
        }
    }

}