package app.simplecloud.npc.paper.command

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.plugin.Plugin
import org.incendo.cloud.SenderMapper

class PaperCommandSenderMapper(
    private val plugin: Plugin,
) : SenderMapper<CommandSourceStack, PaperCommandSender> {
    override fun map(base: CommandSourceStack): PaperCommandSender = PaperCommandSender(plugin, base)
    override fun reverse(mapped: PaperCommandSender): CommandSourceStack = mapped.stack
}
