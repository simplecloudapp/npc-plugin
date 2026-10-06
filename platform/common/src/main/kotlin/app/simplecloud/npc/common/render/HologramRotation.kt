package app.simplecloud.npc.common.render

import app.simplecloud.npc.core.config.NpcConfig
import java.util.concurrent.ConcurrentHashMap

class HologramRotation {

    private val shownFrames = ConcurrentHashMap<String, List<Int>>()

    fun due(configs: Collection<NpcConfig>, now: Long): List<NpcConfig> {
        val rotating = configs.filter { config ->
            config.hologram.layouts.any { layout -> layout.lines.any { it.rotating } }
        }
        shownFrames.keys.retainAll(rotating.mapTo(hashSetOf()) { it.id })

        return rotating.filter { config ->
            val frames = config.hologram.layouts.flatMap { layout -> layout.lines.map { it.frameIndex(now) } }
            shownFrames.put(config.id, frames) != frames
        }
    }
}
