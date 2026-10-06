package app.simplecloud.npc.core.hologram

import app.simplecloud.npc.core.cloud.BridgePlaceholders
import app.simplecloud.npc.core.cloud.CloudOutageLog
import app.simplecloud.npc.core.cloud.CloudUnavailableException
import app.simplecloud.npc.core.cloud.JoinStateResolver
import app.simplecloud.npc.core.cloud.ServerBridgeResolver
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.text.PlayerPlaceholders
import kotlinx.coroutines.CancellationException
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object HologramLineResolver {

    private val miniMessage = MiniMessage.miniMessage()

    class ResolvedLine(val line: HologramLine, val text: Component, val personal: PersonalText?)

    sealed interface Result {
        class Lines(val lines: List<ResolvedLine>) : Result

        data object None : Result
        data object Unavailable : Result
    }

    suspend fun resolve(config: NpcConfig): Result {
        if (!config.hologram.enabled) return Result.None
        if (config.targetServers.isEmpty()) return Result.None

        val bridge = try {
            config.targetServers.firstNotNullOfOrNull { ServerBridgeResolver.resolve(it) } ?: return Result.None
        } catch (_: CloudUnavailableException) {
            return Result.Unavailable
        }

        val layout = config.hologram.layoutShownFor(JoinStateResolver.of(config)) ?: return Result.None

        val now = System.currentTimeMillis()

        return try {
            val resolver = BridgePlaceholders.resolver(bridge)
            Result.Lines(layout.lines.map { line -> resolveLine(line, line.textAt(now), resolver) })
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            val reason = if (exception is CloudUnavailableException) exception.message else exception.toString()
            CloudOutageLog.warnThrottled(
                "Hologram of NPC '${config.id}' not rendered: $reason",
                key = "hologram:${config.id}",
            )
            Result.Unavailable
        }
    }

    private fun resolveLine(line: HologramLine, text: String, resolver: TagResolver): ResolvedLine {
        if (!PlayerPlaceholders.isPersonal(text)) return ResolvedLine(line, miniMessage.deserialize(text, resolver), null)
        val personal = PersonalText.of(text, resolver)

        return ResolvedLine(line, personal.fallback(), personal)
    }
}
