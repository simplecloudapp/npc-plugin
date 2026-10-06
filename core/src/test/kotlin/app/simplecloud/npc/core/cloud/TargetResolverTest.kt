package app.simplecloud.npc.core.cloud

import app.simplecloud.api.group.GroupServerType
import app.simplecloud.api.server.ServerState
import app.simplecloud.npc.core.config.JoinStrategy
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TargetResolverTest {

    private val lobby = CloudFakes.group("Lobby")
    private val proxy = CloudFakes.group("Proxy", GroupServerType.PROXY)
    private val build = CloudFakes.persistentServer("Build")
    private val lobbyServers = listOf(
        CloudFakes.server(lobby, 1, playerCount = 5),
        CloudFakes.server(lobby, 2, playerCount = 2),
        CloudFakes.server(lobby, 3, state = ServerState.STARTING, playerCount = 0),
    )
    private val lists = FakeCloudLists(listOf(lobby, proxy), listOf(build), lobbyServers)

    @Test
    fun `groups and persistent servers resolve by name, case-insensitively`() = runBlocking {
        val group = assertIs<TargetResolution.Found>(TargetResolver.resolve("lobby", lists))
        assertEquals("Lobby", group.target.name)
        assertEquals(TargetType.GROUP, group.target.type)
        assertIs<ServerBridge.OfGroup>(group.target.bridge)

        val persistent = assertIs<TargetResolution.Found>(TargetResolver.resolve("BUILD", lists))
        assertEquals(TargetType.PERSISTENT_SERVER, persistent.target.type)
        assertEquals(TargetResolution.NotFound, TargetResolver.resolve("nope", lists))
    }

    @Test
    fun `a name used by both a group and a persistent server is ambiguous`() = runBlocking {
        val clash = FakeCloudLists(listOf(CloudFakes.group("Hub")), listOf(CloudFakes.persistentServer("hub")))

        assertEquals(TargetResolution.Ambiguous, TargetResolver.resolve("Hub", clash))
        assertNull(ServerBridgeResolver.resolve("Hub", clash))
    }

    @Test
    fun `strategies pick by player count and prefer servers that are not full`() = runBlocking {
        val servers = listOf(
            CloudFakes.server(lobby, 1, playerCount = 5, maxPlayers = 10),
            CloudFakes.server(lobby, 2, playerCount = 2, maxPlayers = 10),
            CloudFakes.server(lobby, 3, playerCount = 10, maxPlayers = 10),
        )
        val withFull = FakeCloudLists(listOf(lobby), servers = servers)

        assertEquals("Lobby-2", TargetResolver.findDestination("Lobby", withFull, JoinStrategy.LEAST_PLAYERS))
        assertEquals("Lobby-1", TargetResolver.findDestination("Lobby", withFull, JoinStrategy.MOST_PLAYERS))
        repeat(20) {
            val picked = TargetResolver.findDestination("Lobby", withFull, JoinStrategy.RANDOM)
            assertTrue(picked in setOf("Lobby-1", "Lobby-2"))
        }
        val allFull = FakeCloudLists(listOf(lobby), servers = listOf(servers[2]))
        assertEquals("Lobby-3", TargetResolver.findDestination("Lobby", allFull))
    }

    @Test
    fun `a persistent server is only a destination while it is joinable`() = runBlocking {
        assertNull(TargetResolver.findDestination("Build", lists))

        val online = FakeCloudLists(persistentServers = listOf(build), servers = listOf(CloudFakes.server(build)))
        assertEquals("Build", TargetResolver.findDestination("Build", online))

        val full = FakeCloudLists(
            persistentServers = listOf(build),
            servers = listOf(CloudFakes.server(build, playerCount = 50, maxPlayers = 50)),
        )
        assertEquals("Build", TargetResolver.findDestination("Build", full))
    }
}
