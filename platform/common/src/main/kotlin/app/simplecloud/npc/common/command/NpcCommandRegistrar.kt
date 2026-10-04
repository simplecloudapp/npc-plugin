package app.simplecloud.npc.common.command

import app.simplecloud.npc.common.command.edit.NpcActionCommand
import app.simplecloud.npc.common.command.edit.NpcHologramCommand
import app.simplecloud.npc.common.command.edit.NpcPushbackCommand
import app.simplecloud.npc.common.command.edit.NpcTargetCommand
import app.simplecloud.npc.common.command.global.InventoryCommand
import app.simplecloud.npc.common.command.global.NpcCommand
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.core.platform.NpcCommandSender

object NpcCommandRegistrar {
    fun <C : NpcCommandSender> commands(pluginContext: NpcPluginContext): List<Any> = listOf(
        CommandSuggestions<C>(pluginContext),
        NpcCommand<C>(pluginContext),
        NpcTargetCommand<C>(pluginContext),
        NpcHologramCommand<C>(pluginContext),
        NpcPushbackCommand<C>(pluginContext),
        NpcActionCommand<C>(pluginContext),
        InventoryCommand<C>(pluginContext),
    )
}
