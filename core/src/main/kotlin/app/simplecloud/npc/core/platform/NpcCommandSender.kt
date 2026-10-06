package app.simplecloud.npc.core.platform

import net.kyori.adventure.text.Component

interface NpcCommandSender {
    fun sendMessage(component: Component)
    fun asPlayer(): NpcPlayer?
    fun hasPermission(permission: String): Boolean
}
