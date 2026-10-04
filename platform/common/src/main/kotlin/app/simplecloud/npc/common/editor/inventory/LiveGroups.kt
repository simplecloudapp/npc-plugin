package app.simplecloud.npc.common.editor.inventory

import app.simplecloud.npc.common.inventory.source.GroupServersSource
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.SourceConfiguration

object LiveGroups {

    class SourceInfo(
        val icon: String,
        val label: String,
        val lines: List<String>,
        val slotMaterial: String,
        val slotName: String,
        val slotLore: List<String>,
    )

    val COLORS = listOf(
        "LIME" to "#a3e635",
        "LIGHT_BLUE" to "#38bdf8",
        "ORANGE" to "#fb923c",
        "MAGENTA" to "#e879f9",
        "YELLOW" to "#facc15",
        "RED" to "#f87171",
        "CYAN" to "#22d3ee",
    )

    private val JOIN_LORE = listOf(
        "<#94a3b8>Players: <#f8fafc><entry_online_players>/<entry_max_players>",
        "",
        "<#475569>Click to join",
    )

    fun names(config: InventoryConfiguration): List<String> =
        config.items.sortedBy { it.slot }.mapNotNull { it.liveGroup }.distinct()

    fun members(config: InventoryConfiguration, group: String): List<InventoryItemConfiguration> =
        config.items.filter { it.liveGroup == group }.sortedBy { it.slot }

    fun contentSlots(config: InventoryConfiguration, group: String): List<Int> =
        members(config, group).filter { it.pageControl == null }.map { it.slot }

    fun colorOf(config: InventoryConfiguration, group: String): Pair<String, String> {
        val index = names(config).indexOf(group).coerceAtLeast(0)
        return COLORS[index % COLORS.size]
    }

    fun source(config: InventoryConfiguration, group: String): SourceConfiguration? =
        members(config, group).firstNotNullOfOrNull { it.live }

    fun whenEmpty(config: InventoryConfiguration, group: String): InventoryItemConfiguration? =
        members(config, group).firstNotNullOfOrNull { it.whenEmpty }

    fun setSource(
        config: InventoryConfiguration,
        group: String,
        transform: (SourceConfiguration) -> SourceConfiguration,
    ): InventoryConfiguration? {
        val current = source(config, group) ?: SourceConfiguration()
        return withShared(config, group, live = transform(current), whenEmpty = whenEmpty(config, group))
    }

    fun setLook(
        config: InventoryConfiguration,
        group: String,
        state: String,
        transform: (InventoryConfiguration.StateLook) -> InventoryConfiguration.StateLook?,
    ): InventoryConfiguration? = setSource(config, group) { source ->
        val looks = source.looks.filterKeys { !it.equals(state, true) }.toMutableMap()
        transform(source.lookFor(state) ?: InventoryConfiguration.StateLook())
            ?.takeUnless { it.isEmpty() }
            ?.let { looks[state] = it }
        source.copy(looks = looks)
    }

    fun setWhenEmpty(
        config: InventoryConfiguration,
        group: String,
        look: InventoryItemConfiguration?,
    ): InventoryConfiguration? =
        withShared(config, group, live = source(config, group), whenEmpty = look?.copy(slot = -1))

    fun rename(config: InventoryConfiguration, group: String, newName: String): InventoryConfiguration? {
        if (newName.isBlank() || newName in names(config)) return null
        val items = config.items.map { if (it.liveGroup == group) it.copy(liveGroup = newName) else it }

        return config.copy(items = items.toMutableList())
    }

    fun delete(config: InventoryConfiguration, group: String): InventoryConfiguration? {
        if (members(config, group).isEmpty()) return null
        return config.copy(items = config.items.filterNot { it.liveGroup == group }.toMutableList())
    }

    fun lacksPageControls(config: InventoryConfiguration, group: String): Boolean =
        members(config, group).none { it.pageControl != null }

    fun freeName(config: InventoryConfiguration, type: String): String {
        val base = when (type) {
            GroupServersSource.TYPE, "persistent-servers" -> "servers"
            "groups" -> "groups"
            else -> "entries"
        }
        val taken = names(config).toSet()

        return generateSequence(1) { it + 1 }.map { if (it == 1) base else "$base-$it" }.first { it !in taken }
    }

    fun newSlot(group: String, type: String): InventoryItemConfiguration {
        val info = describe(type)
        return InventoryItemConfiguration(
            material = info.slotMaterial,
            name = info.slotName,
            lore = info.slotLore.toMutableList(),
            liveGroup = group,
            live = SourceConfiguration(type = type),
        )
    }

    fun describe(type: String): SourceInfo = when (type) {
        GroupServersSource.TYPE -> SourceInfo(
            "GRASS_BLOCK", "Servers Of A Group",
            listOf("Every running server of one group,", "e.g. Lobby-1, Lobby-2, ..."),
            "GRASS_BLOCK", "<#a3e635><entry_group_name>-<entry_numerical_id>", JOIN_LORE,
        )

        "persistent-servers" -> SourceInfo(
            "ENDER_CHEST", "Persistent Servers", listOf("Every persistent server."),
            "ENDER_CHEST", "<#a3e635><entry_name>", JOIN_LORE,
        )

        "groups" -> SourceInfo(
            "CHEST", "Groups", listOf("One entry per server group."),
            "CHEST", "<#a3e635><entry_name>", JOIN_LORE,
        )

        "static" -> SourceInfo(
            "PAPER", "Fixed List", listOf("Entries written by hand in the YAML."),
            "PAPER", "<#e2e8f0>Entry", emptyList(),
        )

        else -> SourceInfo("COMPARATOR", type, listOf("Provided by an addon."), "PAPER", "<#e2e8f0>Entry", emptyList())
    }

    private fun withShared(
        config: InventoryConfiguration,
        group: String,
        live: SourceConfiguration?,
        whenEmpty: InventoryItemConfiguration?,
    ): InventoryConfiguration? {
        val holder = members(config, group).firstOrNull { it.pageControl == null } ?: return null
        val items = config.items.map { item ->
            when {
                item.liveGroup != group -> item
                item.slot == holder.slot -> item.copy(live = live, whenEmpty = whenEmpty)
                else -> item.copy(live = null, whenEmpty = null)
            }
        }

        return config.copy(items = items.toMutableList())
    }
}
