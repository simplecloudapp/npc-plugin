package app.simplecloud.npc.common.editor

object PromptTokens {

    val PLAYER = listOf(
        PromptPlaceholder("<playername>", "The name of the player who clicked."),
        PromptPlaceholder("<playeruuid>", "Their UUID."),
    )

    val TARGET = listOf(
        PromptPlaceholder("<target_name>", "Name of the NPC's target group or server."),
        PromptPlaceholder("<target_online_players>", "Players online on the target."),
        PromptPlaceholder("<target_max_players>", "Player slots of the target."),
        PromptPlaceholder("<target_type>", "Server type of the target."),
        PromptPlaceholder("<target_property:key>", "Any cloud property; replace key with its name."),
    )

    val PAGES = listOf(
        PromptPlaceholder("<page>", "Current page of the first live group."),
        PromptPlaceholder("<max_page>", "Number of pages."),
    )

    val ENTRY = listOf(
        PromptPlaceholder("<entry_name>", "Server or group name."),
        PromptPlaceholder("<entry_group_name>", "Its group."),
        PromptPlaceholder("<entry_numerical_id>", "Number, e.g. 2."),
        PromptPlaceholder("<entry_online_players>", "Players online."),
        PromptPlaceholder("<entry_max_players>", "Player slots."),
        PromptPlaceholder("<entry_state>", "State, e.g. AVAILABLE."),
        PromptPlaceholder("<entry_motd>", "Message of the day."),
        PromptPlaceholder("<entry_server>", "What joining connects to."),
    )

    val MENU = PLAYER + PAGES + TARGET

    val HOLOGRAM = TARGET + PLAYER
}
