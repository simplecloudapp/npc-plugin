package app.simplecloud.npc.common.editor.inventory

import app.simplecloud.npc.common.inventory.source.GroupServersSource
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.EntryStates
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.PageControl
import app.simplecloud.npc.core.inventory.InventoryConfiguration.SourceConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.StateLook

enum class MenuTemplate(val label: String, val icon: String, val description: List<String>) {
    BLANK("Blank", "WHITE_STAINED_GLASS_PANE", listOf("Three empty rows.")),
    SERVER_SELECTOR(
        "Server Selector",
        "COMPASS",
        listOf("Every running server of the NPC's", "target group, with page arrows."),
    ),
    GROUP_OVERVIEW("Group Overview", "CHEST", listOf("One slot per group,", "with its player count.")),
    PERSISTENT_SERVERS("Persistent Servers", "ENDER_CHEST", listOf("One slot per persistent server.")),
    NAVIGATION("Navigation", "OAK_SIGN", listOf("Three fixed buttons that send", "players to a server each.")),
    ;

    fun build(id: String): InventoryConfiguration = when (this) {
        BLANK -> InventoryConfiguration(id = id, rows = 3)
        SERVER_SELECTOR -> serverSelector(id)
        GROUP_OVERVIEW -> liveGrid(
            id, "<#0ea5e9><bold>Groups", "groups", "groups", "CHEST",
            "<#a3e635><entry_name>",
            JOIN_LORE,
        )

        PERSISTENT_SERVERS -> liveGrid(
            id, "<#0ea5e9><bold>Servers", "servers", "persistent-servers", "ENDER_CHEST",
            "<#a3e635><entry_name>",
            JOIN_LORE,
        )

        NAVIGATION -> navigation(id)
    }

    private companion object {
        const val PANE = "GRAY_STAINED_GLASS_PANE"

        val JOIN_LORE = listOf(
            "<#94a3b8>Players: <#f8fafc><entry_online_players>/<entry_max_players>",
            "",
            "<#475569>Click to join",
        )

        fun border(rows: Int): List<InventoryItemConfiguration> {
            val size = rows * 9
            return (0 until size)
                .filter { it < 9 || it >= size - 9 || it % 9 == 0 || it % 9 == 8 }
                .map { InventoryItemConfiguration(slot = it, material = PANE, name = " ", hideTooltip = true) }
        }

        fun withBorder(rows: Int, items: List<InventoryItemConfiguration>): MutableList<InventoryItemConfiguration> {
            val taken = items.map { it.slot }.toSet()
            return (border(rows).filterNot { it.slot in taken } + items).toMutableList()
        }

        fun serverSelector(id: String): InventoryConfiguration {
            val content = (10..16) + (19..25)
            val live = content.mapIndexed { index, slot ->
                InventoryItemConfiguration(
                    slot = slot,
                    material = "GRASS_BLOCK",
                    name = "<#a3e635><entry_group_name>-<entry_numerical_id>",
                    lore = mutableListOf(
                        "<#94a3b8>Players: <#f8fafc><entry_online_players>/<entry_max_players>",
                        "<#94a3b8>State: <#f8fafc><entry_state>",
                        "",
                        "<#475569>Click to join this server",
                    ),
                    liveGroup = "servers",
                    live = if (index == 0) {
                        SourceConfiguration(
                            type = GroupServersSource.TYPE,
                            group = GroupServersSource.TARGET_GROUP,
                            states = mutableListOf("AVAILABLE", "INGAME"),
                            looks = mutableMapOf(
                                EntryStates.FULL to StateLook(
                                    material = "RED_CONCRETE",
                                    name = "<#dc2626><entry_group_name>-<entry_numerical_id>",
                                    lore = mutableListOf(
                                        "<#94a3b8>Players: <#f8fafc><entry_online_players>/<entry_max_players>",
                                        "",
                                        "<#dc2626>This server is full",
                                    ),
                                ),
                                "INGAME" to StateLook(
                                    material = "ORANGE_CONCRETE",
                                    name = "<#f59e0b><entry_group_name>-<entry_numerical_id>",
                                    lore = mutableListOf(
                                        "<#94a3b8>Players: <#f8fafc><entry_online_players>/<entry_max_players>",
                                        "",
                                        "<#f59e0b>A round is running",
                                    ),
                                ),
                            ),
                        )
                    } else null,
                    whenEmpty = if (index == 0) {
                        InventoryItemConfiguration(material = PANE, name = "<#475569>Spinning up...")
                    } else null,
                )
            }
            val controls = listOf(
                InventoryItemConfiguration(
                    slot = 39, material = "ARROW", name = "<#e2e8f0>Previous page",
                    liveGroup = "servers", pageControl = PageControl.PREVIOUS,
                ),
                InventoryItemConfiguration(
                    slot = 41, material = "ARROW", name = "<#e2e8f0>Next page",
                    liveGroup = "servers", pageControl = PageControl.NEXT,
                ),
                InventoryItemConfiguration(
                    slot = 4, material = "NETHER_STAR", name = "<#f8fafc>Join a Server", glowing = true,
                    lore = mutableListOf("<#94a3b8>Joins the emptiest server."),
                    actions = mutableListOf(NpcConfig.ActionConfiguration(joinTarget = true)),
                ),
            )

            return InventoryConfiguration(
                id = id,
                title = "<#0ea5e9><bold>Servers",
                rows = 5,
                items = withBorder(5, live + controls),
            )
        }

        fun liveGrid(
            id: String,
            title: String,
            group: String,
            sourceType: String,
            material: String,
            name: String,
            lore: List<String>,
        ): InventoryConfiguration {
            val live = (10..16).mapIndexed { index, slot ->
                InventoryItemConfiguration(
                    slot = slot,
                    material = material,
                    name = name,
                    lore = lore.toMutableList(),
                    liveGroup = group,
                    live = SourceConfiguration(type = sourceType).takeIf { index == 0 },
                )
            }

            return InventoryConfiguration(id = id, title = title, rows = 3, items = withBorder(3, live))
        }

        fun navigation(id: String): InventoryConfiguration {
            fun button(slot: Int, material: String, label: String, server: String) = InventoryItemConfiguration(
                slot = slot,
                material = material,
                name = "<#a3e635>$label",
                lore = mutableListOf("<#475569>Click to go to $server"),
                actions = mutableListOf(NpcConfig.ActionConfiguration(sendToServer = server)),
            )

            return InventoryConfiguration(
                id = id,
                title = "<#0ea5e9><bold>Navigation",
                rows = 3,
                items = withBorder(
                    3,
                    listOf(
                        button(11, "GRASS_BLOCK", "Lobby", "Lobby"),
                        button(13, "DIAMOND_SWORD", "Survival", "Survival"),
                        button(15, "GOLDEN_APPLE", "Minigames", "Minigames"),
                    ),
                ),
            )
        }
    }
}
