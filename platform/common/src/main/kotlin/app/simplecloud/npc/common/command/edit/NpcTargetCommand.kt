package app.simplecloud.npc.common.command.edit

import app.simplecloud.npc.common.command.AbstractNpcCommand
import app.simplecloud.npc.common.command.COMMAND_PERMISSION
import app.simplecloud.npc.common.command.COMMAND_SYNTAX
import app.simplecloud.npc.common.command.CommandMessages
import app.simplecloud.npc.common.manager.NpcOperationResult
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.text.sendError
import app.simplecloud.npc.common.text.sendInfo
import app.simplecloud.npc.common.text.sendSuccess
import app.simplecloud.npc.common.text.sendWarning
import app.simplecloud.npc.common.utils.BackgroundTasks
import app.simplecloud.npc.core.platform.NpcCommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

class NpcTargetCommand<C : NpcCommandSender>(pluginContext: NpcPluginContext) : AbstractNpcCommand<C>(pluginContext) {

    @Command("$COMMAND_SYNTAX edit <id> target list")
    @Permission(COMMAND_PERMISSION)
    fun list(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return

        sender.sendInfo("Targets for {}: {}", config.id, config.targetServers.joinToString())
    }

    @Command("$COMMAND_SYNTAX edit <id> target add <target>")
    @Permission(COMMAND_PERMISSION)
    fun add(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("target", suggestions = "targets") targetName: String,
    ) {
        val config = findNpc(sender, id) ?: return
        BackgroundTasks.launch {
            when (val result = pluginContext.npcManager.addTarget(config.id, targetName)) {
                is NpcOperationResult.Success ->
                    sender.sendSuccess("Added target {} to NPC {}.", result.config.targetServers.last(), id)

                is NpcOperationResult.Failure -> CommandMessages.sendResult(sender, result)
            }
        }
    }

    @Command("$COMMAND_SYNTAX edit <id> target remove <target>")
    @Permission(COMMAND_PERMISSION)
    fun remove(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("target", suggestions = "configuredTargets") targetName: String,
    ) {
        val config = findNpc(sender, id) ?: return
        val target = config.targetServers.firstOrNull { it.equals(targetName, ignoreCase = true) } ?: run {
            sender.sendWarning("NPC {} does not use target {}.", id, targetName)
            return
        }

        if (config.targetServers.size == 1) {
            sender.sendError("NPC {} must keep at least one target.", id)
            return
        }

        config.targetServers.remove(target)
        saveConfig(sender, config, "Removed target {} from NPC {}.", target, id)
    }
}
