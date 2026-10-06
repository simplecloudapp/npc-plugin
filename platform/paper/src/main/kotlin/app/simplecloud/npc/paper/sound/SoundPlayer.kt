package app.simplecloud.npc.paper.sound

import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.NpcConfig
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

object SoundPlayer {

    private val warned = ConcurrentHashMap.newKeySet<String>()

    fun play(player: Player, name: String, options: NpcConfig.SoundOptions) {
        val key = SoundCatalog.resolve(name)?.toString()
            ?: name.takeIf { '.' in it || ':' in it }
            ?: run {
                if (warned.add(name)) {
                    NpcLog.logger.warning("Unknown sound '$name'; pick one in the editor's sound picker.")
                }
                return
            }
        runCatching { player.playSound(player.location, key, options.volume.toFloat(), options.pitch.toFloat()) }
    }
}
