package app.simplecloud.npc.common.editor.npc

import app.simplecloud.api.server.ServerState
import app.simplecloud.npc.common.editor.core.KnownTarget
import app.simplecloud.npc.common.editor.core.LiveServer
import app.simplecloud.npc.common.editor.core.ServerStatus
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.core.cloud.CloudListCache
import app.simplecloud.npc.core.cloud.CloudSnapshot
import app.simplecloud.npc.core.cloud.CloudSnapshots
import app.simplecloud.npc.core.cloud.JoinStateResolver
import app.simplecloud.npc.core.cloud.connectName
import app.simplecloud.npc.core.cloud.groupName
import app.simplecloud.npc.core.cloud.isProxy
import app.simplecloud.npc.core.config.NpcConfig
import kotlin.math.sqrt

class CloudNpcEditorLiveData(
    private val pluginContextProvider: () -> NpcPluginContext,
    private val snapshots: CloudSnapshots = CloudListCache,
) : NpcEditorLiveData {

    private fun peek(): CloudSnapshot = snapshots.peek()

    override fun cloudKnown(): Boolean = peek().known

    private fun CloudSnapshot.serversOf(target: String) = servers.filter { server ->
        server.groupName().equals(target, true) || server.persistentServer?.name.equals(target, true)
    }

    override fun summaryOf(target: String): TargetSummary {
        val snapshot = peek()
        val servers = snapshot.serversOf(target)

        return TargetSummary(
            isGroup = snapshot.groups.any { it.name.equals(target, true) },
            liveServers = servers.size,
            players = servers.sumOf { it.playerCount ?: 0 },
            known = snapshot.known,
        )
    }

    override fun playersInRange(config: NpcConfig): Int {
        val npc = config.entity.location
        val range = config.entity.viewDistance

        return pluginContextProvider().playerDirectory.onlinePlayers().count { player ->
            val at = player.location()
            at.world == npc.world && distance(at.x - npc.x, at.y - npc.y, at.z - npc.z) <= range
        }
    }

    private fun distance(dx: Double, dy: Double, dz: Double): Double = sqrt(dx * dx + dy * dy + dz * dz)

    override fun joinState(target: String): String? =
        peek().takeIf { it.known }?.let { JoinStateResolver.resolve(target, it) }

    override fun knownTargets(): List<KnownTarget> {
        val snapshot = peek()
        val groups = snapshot.groups.filterNot { it.isProxy() }.map { KnownTarget(it.name, group = true) }
        val persistent =
            snapshot.persistentServers.filterNot { it.isProxy() }.map { KnownTarget(it.name, group = false) }

        return (groups + persistent).distinctBy { it.name.lowercase() }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    override fun servers(): List<LiveServer> {
        val snapshot = peek()
        if (!snapshot.known) return emptyList()

        val running = snapshot.servers.mapNotNull { server ->
            val name = server.connectName() ?: server.persistentServer?.name ?: return@mapNotNull null
            val status = when (server.state) {
                ServerState.AVAILABLE, ServerState.INGAME -> ServerStatus.ONLINE
                ServerState.PREPARING, ServerState.STARTING -> ServerStatus.STARTING
                ServerState.STOPPING, ServerState.CLEANUP -> ServerStatus.STOPPING
                else -> ServerStatus.UNKNOWN
            }

            LiveServer(name, status, playerCount = server.playerCount ?: 0)
        }
        val runningNames = running.mapTo(HashSet()) { it.name.lowercase() }
        val down = snapshot.persistentServers
            .filterNot { it.isProxy() || it.name.lowercase() in runningNames }
            .map { LiveServer(it.name, ServerStatus.OFFLINE, playerCount = 0) }

        return (running + down).sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
}
