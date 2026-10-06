package app.simplecloud.npc.common.editor.ui

import app.simplecloud.npc.core.interaction.PlayerInteraction

object Clicks {

    fun short(interaction: PlayerInteraction): String = when (interaction) {
        PlayerInteraction.LEFT_CLICK -> "Left-click"
        PlayerInteraction.RIGHT_CLICK -> "Right-click"
        PlayerInteraction.SHIFT_LEFT_CLICK -> "Shift+Left"
        PlayerInteraction.SHIFT_RIGHT_CLICK -> "Shift+Right"
    }

    fun label(interaction: PlayerInteraction): String = when (interaction) {
        PlayerInteraction.LEFT_CLICK -> "Left-click"
        PlayerInteraction.RIGHT_CLICK -> "Right-click"
        PlayerInteraction.SHIFT_LEFT_CLICK -> "Shift + Left-click"
        PlayerInteraction.SHIFT_RIGHT_CLICK -> "Shift + Right-click"
    }

    fun material(interaction: PlayerInteraction): String = when (interaction) {
        PlayerInteraction.LEFT_CLICK -> "STONE_BUTTON"
        PlayerInteraction.SHIFT_LEFT_CLICK -> "POLISHED_BLACKSTONE_BUTTON"
        PlayerInteraction.RIGHT_CLICK -> "OAK_BUTTON"
        PlayerInteraction.SHIFT_RIGHT_CLICK -> "CRIMSON_BUTTON"
    }
}
