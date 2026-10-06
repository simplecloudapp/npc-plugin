package app.simplecloud.npc.core.cloud

import app.simplecloud.api.server.ServerState
import app.simplecloud.npc.core.config.NpcConfig
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class JoinStateResolverTest {

    private val plain = CloudFakes.group("Plain")
    private val running = listOf(CloudFakes.server(plain, 1, playerCount = 3, maxPlayers = 20))

    @Test
    fun `no joinable server reads as offline once the grace period is over`() = runBlocking {
        val lists = FakeCloudLists(
            groups = listOf(plain),
            servers = listOf(CloudFakes.server(plain, 1, state = ServerState.STARTING)),
        )
        val config = NpcConfig(id = "grace", targetServers = mutableListOf("Plain"))

        assertEquals(NpcConfig.DEFAULT_JOIN_STATE, JoinStateResolver.of(config, lists, now = 1_000))
        assertEquals(NpcConfig.DEFAULT_JOIN_STATE, JoinStateResolver.of(config, lists, now = 5_999))
        assertEquals(JoinStateResolver.OFFLINE, JoinStateResolver.of(config, lists, now = 6_000))
        assertEquals(JoinStateResolver.OFFLINE, JoinStateResolver.resolve(config, lists.asSnapshot()))
    }

    @Test
    fun `one joinable server on any target keeps the NPC public`() {
        val farm = CloudFakes.group("Farm")
        val snapshot = FakeCloudLists(
            groups = listOf(plain, farm),
            servers = listOf(CloudFakes.server(farm, 1, state = ServerState.AVAILABLE)),
        ).asSnapshot()
        val both = NpcConfig(id = "both", targetServers = mutableListOf("Plain", "Farm"))

        assertEquals(JoinStateResolver.OFFLINE, JoinStateResolver.resolve("Plain", snapshot))
        assertEquals(NpcConfig.DEFAULT_JOIN_STATE, JoinStateResolver.resolve(both, snapshot))
    }

    @Test
    fun `an outage or a cold cache reads as the default state`() = runBlocking {
        val config = NpcConfig(id = "outage", targetServers = mutableListOf("Plain"))
        assertEquals(NpcConfig.DEFAULT_JOIN_STATE, JoinStateResolver.of(config, UnreachableCloud))
        val coldCache = CloudSnapshot(groups = listOf(plain))
        assertEquals(NpcConfig.DEFAULT_JOIN_STATE, JoinStateResolver.resolve(config, coldCache))
    }
}
