package app.simplecloud.npc.core.inventory

import app.simplecloud.api.server.Server
import app.simplecloud.api.server.ServerState
import app.simplecloud.npc.core.cloud.isFull
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.plugin.api.shared.config.VersionedConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class InventoryConfiguration(
    override val version: Int = CURRENT_VERSION,
    val id: String = "",
    val title: String = "<#0ea5e9>Menu",
    val rows: Int = 3,
    val items: MutableList<InventoryItemConfiguration> = mutableListOf(),
    @Setting("open-permission")
    val openPermission: String? = null,
    @Setting("open-sound")
    val openSound: String? = null,
) : VersionedConfig {
    fun size(): Int = rows.coerceIn(ROWS) * 9

    fun liveGroups(): List<LiveGroup> =
        items.filter { it.liveGroup != null }.groupBy { it.liveGroup!! }.map { (groupId, members) ->
            LiveGroup(
                id = groupId,
                sourceType = members.firstNotNullOfOrNull { it.live }?.type,
                contentSlots = members.count { it.pageControl == null },
                paginated = members.any { it.pageControl != null },
            )
        }

    fun deepCopy(): InventoryConfiguration = copy(items = items.map { it.deepCopy() }.toMutableList())

    data class LiveGroup(val id: String, val sourceType: String?, val contentSlots: Int, val paginated: Boolean)

    object PageControl {
        const val PREVIOUS = "previous"
        const val NEXT = "next"
    }

    companion object {
        const val CURRENT_VERSION = 1
        const val MIN_ROWS = 1
        const val MAX_ROWS = 6
        val ROWS = MIN_ROWS..MAX_ROWS
    }

    @ConfigSerializable
    data class InventoryItemConfiguration(
        val slot: Int = -1,
        val material: String = "STONE",
        @Setting("custom-model-data")
        val customModelData: Int? = null,
        val name: String? = null,
        val lore: MutableList<String> = mutableListOf(),
        val amount: Int = 1,
        val glowing: Boolean = false,
        @Setting("hide-tooltip")
        val hideTooltip: Boolean = false,
        val head: HeadConfiguration? = null,
        val actions: MutableList<NpcConfig.ActionConfiguration> = mutableListOf(),
        @Setting("view-permission")
        val viewPermission: String? = null,

        @Setting("live-group")
        val liveGroup: String? = null,
        val live: SourceConfiguration? = null,
        @Setting("when-empty")
        val whenEmpty: InventoryItemConfiguration? = null,
        @Setting("page-control")
        val pageControl: String? = null,
    ) {
        fun deepCopy(): InventoryItemConfiguration = copy(
            lore = lore.toMutableList(),
            actions = actions.map { it.copy() }.toMutableList(),
            live = live?.deepCopy(),
            whenEmpty = whenEmpty?.deepCopy(),
        )
    }

    @ConfigSerializable
    data class HeadConfiguration(
        val texture: String = "",
        val signature: String? = null,
        val viewer: Boolean? = null,
        val owner: String? = null,
    ) {
        val showsViewer: Boolean get() = viewer == true
    }

    @ConfigSerializable
    data class SourceConfiguration(
        val type: String = DEFAULT_TYPE,
        val group: String? = null,
        val states: MutableList<String> = mutableListOf(),
        val sort: String? = null,
        val entries: MutableList<InventoryItemConfiguration> = mutableListOf(),
        val looks: MutableMap<String, StateLook> = mutableMapOf(),
    ) {
        fun deepCopy(): SourceConfiguration = copy(
            states = states.toMutableList(),
            entries = entries.map { it.deepCopy() }.toMutableList(),
            looks = looks.mapValues { it.value.copy(lore = it.value.lore?.toMutableList()) }.toMutableMap(),
        )

        fun lookFor(state: String?): StateLook? =
            state?.let { wanted -> looks.entries.firstOrNull { it.key.equals(wanted, true) }?.value }

        companion object {
            const val DEFAULT_TYPE = "group-servers"
        }
    }

    @ConfigSerializable
    data class StateLook(
        val material: String? = null,
        val name: String? = null,
        val lore: MutableList<String>? = null,
        val glowing: Boolean? = null,
    ) {
        fun isEmpty(): Boolean = material == null && name == null && lore == null && glowing == null
    }

    object EntryStates {
        const val FULL = "FULL"
        const val OFFLINE = "OFFLINE"

        val SERVER_STATES = listOf(ServerState.AVAILABLE.name, FULL, ServerState.STARTING.name, ServerState.INGAME.name)
        val TARGET_STATES = listOf(ServerState.AVAILABLE.name, FULL, ServerState.STARTING.name, OFFLINE)

        fun of(servers: List<Server>): String {
            val joinable = servers.filter { it.state == ServerState.AVAILABLE }
            return when {
                joinable.isNotEmpty() -> if (joinable.all(Server::isFull)) FULL else ServerState.AVAILABLE.name
                servers.any { it.state == ServerState.STARTING || it.state == ServerState.PREPARING } ->
                    ServerState.STARTING.name

                else -> OFFLINE
            }
        }

        fun of(server: Server): String? = when {
            server.state == ServerState.AVAILABLE && server.isFull() -> FULL
            server.state == ServerState.PREPARING -> ServerState.STARTING.name
            else -> server.state?.name
        }
    }
}
