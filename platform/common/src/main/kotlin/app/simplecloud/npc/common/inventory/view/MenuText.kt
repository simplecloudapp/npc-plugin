package app.simplecloud.npc.common.inventory.view

import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.text.PlayerPlaceholders
import app.simplecloud.npc.common.text.substitute
import app.simplecloud.npc.core.cloud.BridgePlaceholders
import app.simplecloud.npc.core.cloud.ServerBridge
import app.simplecloud.npc.core.platform.NpcPlayer
import kotlinx.coroutines.CancellationException

class MenuText(
    private val player: NpcPlayer,
    private val target: ServerBridge?,
    private val pages: Pair<Int, Int>?,
) {
    suspend fun resolve(text: String): String {
        if (text.isEmpty()) return text
        val pageValues = mapOf(
            "<page>" to (pages?.first ?: 1).toString(),
            "<max_page>" to (pages?.second ?: 1).toString(),
        )
        val resolved = PlayerPlaceholders.resolve(text, player).substitute(pageValues)
        if (target == null || "<target_" !in resolved) return resolved

        return try {
            Msg.miniMessage.serialize(BridgePlaceholders.append(target, resolved))
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            resolved
        }
    }
}
