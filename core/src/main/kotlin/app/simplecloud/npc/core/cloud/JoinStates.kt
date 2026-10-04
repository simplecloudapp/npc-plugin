package app.simplecloud.npc.core.cloud

import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import app.simplecloud.npc.core.config.NpcConfig
import java.util.concurrent.ConcurrentHashMap

object JoinStateResolver {
    const val OFFLINE = "offline"

    private const val OFFLINE_GRACE_MILLIS = 5_000L

    private class OfflineStreak(val since: Long, val lastSeen: Long)

    private val offline = ConcurrentHashMap<String, OfflineStreak>()

    suspend fun of(
        config: NpcConfig,
        lists: CloudLists = CloudListCache,
        now: Long = System.currentTimeMillis(),
    ): String {
        if (config.targetServers.isEmpty()) return NpcConfig.DEFAULT_JOIN_STATE
        val state = try {
            resolve(config, snapshotOf(lists))
        } catch (_: CloudUnavailableException) {
            NpcConfig.DEFAULT_JOIN_STATE
        }

        return settle(config.id, state, now)
    }

    fun resolve(config: NpcConfig, snapshot: CloudSnapshot): String {
        val bridges = config.targetServers.mapNotNull { ServerBridgeResolver.resolve(it, snapshot) }
        bridges.firstNotNullOfOrNull(::property)?.let { return it }
        if (!snapshot.known || bridges.isEmpty()) return NpcConfig.DEFAULT_JOIN_STATE

        return availability(bridges.flatMap { serversOf(it, snapshot.servers) })
    }

    fun resolve(target: String, snapshot: CloudSnapshot): String {
        val bridge = ServerBridgeResolver.resolve(target, snapshot) ?: return NpcConfig.DEFAULT_JOIN_STATE
        property(bridge)?.let { return it }
        if (!snapshot.known) return NpcConfig.DEFAULT_JOIN_STATE

        return availability(serversOf(bridge, snapshot.servers))
    }

    private fun property(bridge: ServerBridge): String? {
        val value = bridge.properties["join-state"] ?: bridge.properties["joinstate"]
        return (value as? String)?.lowercase()?.takeUnless { it == NpcConfig.DEFAULT_JOIN_STATE }
    }

    private fun availability(servers: List<Server>): String =
        if (servers.none { it.state == ServerState.AVAILABLE }) OFFLINE else NpcConfig.DEFAULT_JOIN_STATE

    private fun settle(npcId: String, state: String, now: Long): String {
        if (state != OFFLINE) {
            offline.remove(npcId)
            return state
        }
        val streak = offline.compute(npcId) { _, previous ->
            val since = previous?.takeIf { now - it.lastSeen <= OFFLINE_GRACE_MILLIS }?.since ?: now
            OfflineStreak(since, now)
        }!!

        return if (now - streak.since >= OFFLINE_GRACE_MILLIS) OFFLINE else NpcConfig.DEFAULT_JOIN_STATE
    }

    private suspend fun snapshotOf(lists: CloudLists): CloudSnapshot =
        lists as? CloudSnapshot
            ?: CloudSnapshot(lists.groups(), lists.persistentServers(), lists.servers(), known = true)
}

object JoinStates {
    private val WELL_KNOWN = listOf(
        NpcConfig.DEFAULT_JOIN_STATE,
        "maintenance",
        JoinStateResolver.OFFLINE,
    )

    fun offered(config: NpcConfig, liveStates: Collection<String> = emptyList()): List<String> {
        val layoutStates = config.hologram.layouts.map { it.joinState }
        val actionStates = config.actions.map { it.joinState }

        return (WELL_KNOWN + liveStates + layoutStates + actionStates + config.joinStates)
            .map { it.lowercase() }
            .distinct()
    }

    fun live(config: NpcConfig, snapshot: CloudSnapshot): List<String> =
        config.targetServers.map { target -> JoinStateResolver.resolve(target, snapshot) }.distinct()
}
