package app.simplecloud.npc.plugin.paper.command.edit

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.COMMAND_PERMISSION
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.shared.bridge.TargetResolution
import app.simplecloud.npc.shared.bridge.TargetResolver
import app.simplecloud.npc.shared.manager.NpcFailure
import app.simplecloud.npc.shared.manager.NpcOperationResult
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.plugin.api.shared.extension.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.runBlocking
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

class NpcTargetCommand(namespace: NpcNamespace) : AbstractNpcCommand(namespace) {

    @Command("$commandName edit <id> target list")
    @Permission(COMMAND_PERMISSION)
    fun list(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpcConfig(sender.sender, id) ?: return
        sender.sender.sendMessage(
            text("$PREFIX <#0ea5e9>Targets for <#f8fafc>${config.id}<#475569>: <#e2e8f0>${config.targetServers.joinToString()}")
        )
    }

    @Command("$commandName edit <id> target add <target>")
    @Permission(COMMAND_PERMISSION)
    fun add(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("target", suggestions = "targets") targetName: String,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val target = when (val resolution = runBlocking { TargetResolver.resolve(targetName) }) {
            is TargetResolution.Found -> resolution.target.name
            TargetResolution.Ambiguous -> {
                CommandMessages.sendResult(sender.sender, NpcOperationResult.Failure(NpcFailure.TARGET_AMBIGUOUS, targetName), "")
                return
            }
            TargetResolution.NotFound -> {
                CommandMessages.sendResult(sender.sender, NpcOperationResult.Failure(NpcFailure.TARGET_NOT_FOUND, targetName), "")
                return
            }
        }
        if (config.targetServers.any { it.equals(target, true) }) {
            sender.sender.sendMessage(text("$PREFIX <#f59e0b>NPC <#f8fafc>$id <#f59e0b>already uses target <#f8fafc>$target<#f59e0b>."))
            return
        }
        config.targetServers += target
        saveConfig(sender.sender, config, "Added target <#f8fafc>$target <#a3e635>to NPC <#f8fafc>$id<#a3e635>.")
    }

    @Command("$commandName edit <id> target remove <target>")
    @Permission(COMMAND_PERMISSION)
    fun remove(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("target", suggestions = "configuredTargets") targetName: String,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val target = config.targetServers.firstOrNull { it.equals(targetName, true) }
        if (target == null) {
            sender.sender.sendMessage(text("$PREFIX <#f59e0b>NPC <#f8fafc>$id <#f59e0b>does not use target <#f8fafc>$targetName<#f59e0b>."))
            return
        }
        if (config.targetServers.size == 1) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>NPC <#f8fafc>$id <#dc2626>must keep at least one target."))
            return
        }
        config.targetServers.remove(target)
        saveConfig(sender.sender, config, "Removed target <#f8fafc>$target <#a3e635>from NPC <#f8fafc>$id<#a3e635>.")
    }
}
