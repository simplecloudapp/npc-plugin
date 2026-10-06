package app.simplecloud.npc.common.command

import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.text.PlayerPlaceholders
import java.util.Locale

object PlaceholderPage {

    private class Token(val token: String, val description: String)

    private class Section(val title: String, val tokens: List<Token>)

    private class Category(
        val key: String,
        val title: String,
        val usedIn: String,
        val sections: () -> List<Section>,
        val note: String? = null,
    )

    private const val TEXT = "<#e2e8f0>"
    private const val KEY = "<#38bdf8>"
    private const val STATES = "STATE: all, AVAILABLE, STARTING, INGAME, STOPPING"

    private val CATEGORIES = listOf(
        Category(
            key = "target",
            title = "Target",
            usedIn = "Holograms and menus, filled with the NPC's group or server",
            sections = {
                listOf(
                    Section(
                        "Every target",
                        listOf(
                            Token("<target_name>", "Name"),
                            Token("<target_online_players>", "Players online"),
                            Token("<target_max_players>", "Player slots"),
                            Token("<target_type>", "Server type"),
                            Token("<target_min_memory>", "Minimum memory"),
                            Token("<target_max_memory>", "Maximum memory"),
                            Token("<target_property:key>", "Any cloud property"),
                        ),
                    ),
                    Section(
                        "Groups",
                        listOf(
                            Token("<target_server_count:STATE>", "Servers in STATE"),
                            Token("<target_player_count:STATE>", "Players in STATE"),
                        ),
                    ),
                    Section(
                        "Persistent servers",
                        listOf(
                            Token("<target_id>", "Server id"),
                            Token("<target_pretty_name>", "Display name"),
                            Token("<target_motd>", "MOTD"),
                        ),
                    ),
                )
            },
            note = STATES,
        ),
        Category(
            key = "player",
            title = "Player",
            usedIn = "Holograms, action texts, commands and menus",
            sections = {
                listOfNotNull(
                    Section(
                        "The player",
                        listOf(
                            Token("<playername>", "Name"),
                            Token("<playeruuid>", "UUID"),
                        ),
                    ),
                    Section(
                        "PlaceholderAPI",
                        listOf(Token("%player_level%", "Any PlaceholderAPI placeholder")),
                    ).takeIf { PlayerPlaceholders.external != null },
                )
            },
            note = "Holograms show them per player.",
        ),
        Category(
            key = "menu",
            title = "Menu",
            usedIn = "Menu titles, item names and lore",
            sections = {
                listOf(
                    Section(
                        "Pages",
                        listOf(
                            Token("<page>", "Current page"),
                            Token("<max_page>", "Number of pages"),
                        ),
                    ),
                )
            },
            note = "Player and target placeholders work here too.",
        ),
        Category(
            key = "entry",
            title = "Live entry",
            usedIn = "Items in live slots, filled once per listed server",
            sections = {
                listOf(
                    Section(
                        "Every entry",
                        listOf(
                            Token("<entry_name>", "Name"),
                            Token("<entry_online_players>", "Players online"),
                            Token("<entry_max_players>", "Player slots"),
                            Token("<entry_type>", "Server type"),
                            Token("<entry_property:key>", "Any cloud property"),
                        ),
                    ),
                    Section(
                        "Servers of a group",
                        listOf(
                            Token("<entry_server>", "Join destination"),
                            Token("<entry_group>", "Group name"),
                            Token("<entry_numerical_id>", "Number, e.g. 2"),
                            Token("<entry_state>", "Server state"),
                            Token("<entry_motd>", "MOTD"),
                            Token("<entry_ip>", "Address"),
                            Token("<entry_port>", "Port"),
                        ),
                    ),
                    Section(
                        "Groups",
                        listOf(
                            Token("<entry_group>", "Group name"),
                            Token("<entry_server_count:STATE>", "Servers in STATE"),
                            Token("<entry_player_count:STATE>", "Players in STATE"),
                        ),
                    ),
                    Section(
                        "Persistent servers",
                        listOf(
                            Token("<entry_server>", "Join destination"),
                            Token("<entry_pretty_name>", "Display name"),
                            Token("<entry_motd>", "MOTD"),
                        ),
                    ),
                )
            },
            note = STATES,
        ),
    )

    val topics: List<String> = CATEGORIES.map { it.key }

    fun render(topic: String?): String? {
        val category = topic?.let { key -> CATEGORIES.firstOrNull { it.key == key.lowercase(Locale.ROOT) } }
            ?: CATEGORIES.first().takeIf { topic == null }
            ?: return null
        val sections = category.sections()
        val width = sections.flatMap { it.tokens }.maxOf { ChatPixels.width(it.token) }

        return buildList {
            add("${Msg.infoPrefix()}Placeholders  ${tabs(category)}")
            add("  ${Msg.MUTED}${category.usedIn}")
            sections.forEach { section ->
                add("")
                add("  $KEY${section.title}")
                section.tokens.forEach { add("    ${tokenRow(it, width)}") }
            }
            add("")
            category.note?.let { add("  ${Msg.SUBTLE}$it") }
            add("  ${Msg.SUBTLE}Click a placeholder to copy it.")
        }.joinToString("\n")
    }

    private fun tabs(active: Category): String = CATEGORIES.joinToString(" ${Msg.SUBTLE}· ") { category ->
        if (category == active) {
            "${Msg.ACCENT}<u>${category.title}</u>"
        } else {
            val command = "/$COMMAND_LABEL placeholders ${category.key}"
            val tip = "${Msg.MUTED}${category.usedIn}"
            "<click:run_command:'${arg(command)}'><hover:show_text:'${arg(tip)}'>" +
                "${Msg.MUTED}${category.title}</hover></click>"
        }
    }

    private fun tokenRow(token: Token, width: Int): String {
        val padding = " ".repeat(((width - ChatPixels.width(token.token)).coerceAtLeast(0) + 3) / 4)
        val tip = "$TEXT${esc(token.token)}\n${Msg.MUTED}${esc(token.description)}\n\n${Msg.SUBTLE}Click to copy"

        return "<click:copy_to_clipboard:'${arg(token.token)}'><hover:show_text:'${arg(tip)}'>" +
            "$TEXT${esc(token.token)}$padding  ${Msg.MUTED}${esc(token.description)}</hover></click>"
    }

    private fun arg(text: String) = text.replace("\\", "\\\\").replace("'", "\\'")

    private fun esc(text: String) = Msg.miniMessage.escapeTags(text)
}
