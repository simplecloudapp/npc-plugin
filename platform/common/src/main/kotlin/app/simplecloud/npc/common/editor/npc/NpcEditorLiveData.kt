package app.simplecloud.npc.common.editor.npc

import app.simplecloud.npc.common.editor.core.KnownTarget
import app.simplecloud.npc.common.editor.core.LiveServer
import app.simplecloud.npc.core.config.NpcConfig

class HologramLiveState(val joinState: String?, val shown: String?) {
    val fallbackInUse: Boolean get() = shown != null && joinState != null && shown != joinState
}

class TargetSummary(
    val isGroup: Boolean,
    val liveServers: Int,
    val players: Int,
    val known: Boolean = true,
)

interface NpcEditorLiveData {
    fun cloudKnown(): Boolean = true
    fun summaryOf(target: String): TargetSummary
    fun playersInRange(config: NpcConfig): Int
    fun joinState(target: String): String?
    fun knownTargets(): List<KnownTarget>
    fun servers(): List<LiveServer>

    fun hologramState(config: NpcConfig): HologramLiveState {
        val joinState = config.targetServers.firstOrNull()?.let { joinState(it) }
        val shown = joinState
            ?.takeIf { config.hologram.enabled }
            ?.let { config.hologram.layoutShownFor(it)?.joinState?.lowercase() }

        return HologramLiveState(joinState, shown)
    }
}
