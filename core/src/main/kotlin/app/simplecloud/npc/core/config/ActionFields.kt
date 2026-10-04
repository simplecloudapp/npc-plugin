package app.simplecloud.npc.core.config

object ActionFields {
    const val JOIN_TARGET = "join-target"
    const val OPEN_INVENTORY = "open-inventory"
    const val PLAY_SOUND = "play-sound"
    const val EXECUTE_COMMAND = "execute-command"
    const val SEND_MESSAGE = "send-message"
    const val TELEPORT = "teleport"
    const val SEND_TITLE = "send-title"
    const val SEND_TO_SERVER = "send-to-server"
    const val TRANSFER_TO_SERVER = "transfer-to-server"
    const val ACTION_BAR = "action-bar"
    const val CLOSE_MENU = "close-menu"
    const val PREVIOUS_MENU = "previous-menu"
    const val NPC_ANIMATION = "npc-animation"
    const val SPEECH = "speech"

    val NAMES: List<String> = listOf(
        JOIN_TARGET, OPEN_INVENTORY, PLAY_SOUND, EXECUTE_COMMAND, SEND_MESSAGE,
        TELEPORT, SEND_TITLE, ACTION_BAR, SEND_TO_SERVER, TRANSFER_TO_SERVER,
    )

    val MENU_ONLY: List<String> = listOf(CLOSE_MENU, PREVIOUS_MENU)
    val NPC_ONLY: List<String> = listOf(NPC_ANIMATION, SPEECH)
}
