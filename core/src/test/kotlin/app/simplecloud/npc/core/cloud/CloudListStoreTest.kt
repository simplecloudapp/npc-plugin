package app.simplecloud.npc.core.cloud

import app.simplecloud.api.group.Group
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.api.server.Server
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CloudListStoreTest {

    private val lobby = CloudFakes.group("lobby")
    private val hub = CloudFakes.persistentServer("hub")
    private val lobbyOne = CloudFakes.server(lobby, 1)

    private var clock = 1_000L
    private var groupCalls = 0
    private var serverCalls = 0
    private var fail = false
    private var groupsOnController = listOf(lobby)
    private var availableOnController = listOf(lobbyOne)

    private fun store(ttl: Long = 2_500L) = CloudListStore(
        fetchGroups = { groupCalls++; if (fail) error("controller offline") else groupsOnController },
        fetchPersistentServers = { if (fail) error("controller offline") else listOf(hub) },
        fetchServers = { serverCalls++; if (fail) error("controller offline") else listOf(lobbyOne) },
        fetchAvailableServers = { availableOnController },
        now = { clock },
        ttlMillis = ttl,
        launchInBackground = { block -> runBlocking { block() } },
    )

    @Test
    fun `a downed controller is probed at most once per ttl`() {
        val store = store()
        fail = true
        assertFailsWith<CloudUnavailableException> { runBlocking { store.groups() } }
        val afterFirstAttempt = groupCalls

        repeat(10) { store.peek() }

        assertEquals(afterFirstAttempt, groupCalls, "no probe inside the failure window")

        clock += 3_000
        store.peek()
        assertEquals(afterFirstAttempt + 1, groupCalls, "one probe once the window passed")
    }

    @Test
    fun `refreshNow keeps the data when the controller is down`() {
        val store = store()
        runBlocking { store.warmUp() }
        fail = true

        runBlocking { store.refreshNow() }

        assertContentEquals(listOf(lobby), store.peek().groups, "a failed refresh must not blank the snapshot")
    }

    @Test
    fun `a failing revalidation neither loses the data nor hammers the controller`() {
        val store = store()
        runBlocking { store.warmUp() }
        clock += 3_000
        fail = true

        val groups: List<Group> = store.peek().groups
        val servers: List<Server> = store.peek().servers
        val persistent: List<PersistentServer> = store.peek().persistentServers

        assertContentEquals(listOf(lobby), groups)
        assertContentEquals(listOf(lobbyOne), servers)
        assertContentEquals(listOf(hub), persistent)
        assertEquals(2, groupCalls, "one failed attempt, then the failure window holds")
        assertEquals(
            1,
            serverCalls,
            "the failure closed the window before the second slot was probed - the controller is " +
                "down, so there is nothing to gain from asking it again in the same breath",
        )

        clock += 3_000
        store.peek()
        assertEquals(3, groupCalls, "past the window it is probed again")
    }
}
