package app.simplecloud.npc.paper.command

import app.simplecloud.npc.core.platform.NpcCommandSender
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.paper.player.PaperNpcPlayer
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class PaperCommandSender(
    private val plugin: Plugin,
    val stack: CommandSourceStack,
) : NpcCommandSender {
    private val sender: CommandSender get() = stack.sender

    override fun sendMessage(component: Component) = sender.sendMessage(component)
    override fun asPlayer(): NpcPlayer? = (sender as? Player)?.let { PaperNpcPlayer(plugin, it) }
    override fun hasPermission(permission: String): Boolean = sender.hasPermission(permission)
}
