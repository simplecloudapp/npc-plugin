package app.simplecloud.npc.common.text

import app.simplecloud.npc.common.command.CommandMessages
import app.simplecloud.npc.core.platform.NpcPlayer

object PlayerMessages {
    const val NO_PERMISSION = "player.action.no-permission"

    val PATHS = setOf(NO_PERMISSION)

    fun denied(player: NpcPlayer, custom: String?) {
        if (custom != null) {
            if (custom.isNotBlank()) player.sendMessage(Msg.miniMessage.deserialize(custom))
            return
        }
        CommandMessages.send(player, NO_PERMISSION, "${Msg.errorPrefix()}You are not allowed to do that.")
    }
}
