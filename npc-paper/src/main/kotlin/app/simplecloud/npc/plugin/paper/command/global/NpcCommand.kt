package app.simplecloud.npc.plugin.paper.command.global

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.COMMAND_PERMISSION
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.shared.manager.NpcOperationResult
import app.simplecloud.npc.shared.bridge.TargetResolution
import app.simplecloud.npc.shared.bridge.TargetResolver
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.provider.NpcProviderType
import app.simplecloud.plugin.api.shared.extension.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

class NpcCommand(
    namespace: NpcNamespace,
) : AbstractNpcCommand(namespace) {

    @Command(commandName)
    @Permission(COMMAND_PERMISSION)
    fun root(sender: CommandSourceStack) = CommandMessages.sendHelp(sender.sender)

    @Command("$commandName help")
    @Permission(COMMAND_PERMISSION)
    fun help(sender: CommandSourceStack) = CommandMessages.sendHelp(sender.sender)

    @Command("$commandName help <topic>")
    @Permission(COMMAND_PERMISSION)
    fun helpTopic(
        sender: CommandSourceStack,
        @Argument("topic", suggestions = "helpTopics") topic: String,
    ) = CommandMessages.sendHelp(sender.sender, topic)

    @Command("$commandName placeholders")
    @Permission(COMMAND_PERMISSION)
    fun placeholders(sender: CommandSourceStack) {
        sender.sender.sendMessage(
            text("$PREFIX <#0ea5e9>Target placeholders").appendNewline()
                .append(text("<#e2e8f0><target_name>, <target_type>, <target_online_players>, <target_max_players>")).appendNewline()
                .append(text("<#e2e8f0><target_min_memory>, <target_max_memory>, <target_property:key>")).appendNewline()
                .append(text("<#94a3b8>Persistent server: <target_id>, <target_pretty_name>, <target_motd>"))
        )
    }

    @Command("$commandName edit <id>")
    @Permission(COMMAND_PERMISSION)
    fun edit(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpcConfig(sender.sender, id) ?: return
        sender.sender.sendMessage(text("$PREFIX <#0ea5e9>Edit <#f8fafc>${config.id}<#475569>: <#e2e8f0>target, hologram, action, or pushback"))
    }

    @Command("$commandName create <id> <target>")
    @Permission(COMMAND_PERMISSION)
    fun create(
        sender: CommandSourceStack,
        @Argument("id") id: String,
        @Argument("target", suggestions = "targets") target: String,
    ) = create(sender, id, target, null)

    @Command("$commandName create <id> <target> <provider>")
    @Permission(COMMAND_PERMISSION)
    fun createWithProvider(
        sender: CommandSourceStack,
        @Argument("id") id: String,
        @Argument("target", suggestions = "targets") target: String,
        @Argument("provider", suggestions = "creationProviders") provider: String,
    ) = create(sender, id, target, provider)

    private fun create(sender: CommandSourceStack, id: String, target: String, providerName: String?) {
        val player = sender.sender as? Player ?: run {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>This command can only be used by a player."))
            return
        }
        val provider = providerName?.let(NpcProviderType::getOrNull)
        if (providerName != null && provider == null) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Unknown provider <#f8fafc>$providerName<#dc2626>."))
            return
        }
        val result = runBlocking { namespace.npcManager.create(id, target, provider, player) }
        CommandMessages.sendResult(player, result, "Created NPC")
    }

    @Command("$commandName link <id> <provider> <reference> <target>")
    @Permission(COMMAND_PERMISSION)
    fun link(
        sender: CommandSourceStack,
        @Argument("id") id: String,
        @Argument("provider", suggestions = "providers") providerName: String,
        @Argument("reference", suggestions = "providerReferences") reference: String,
        @Argument("target", suggestions = "targets") target: String,
    ) {
        val provider = NpcProviderType.getOrNull(providerName) ?: run {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Unknown provider <#f8fafc>$providerName<#dc2626>."))
            return
        }
        CommandMessages.sendResult(
            sender.sender,
            runBlocking { namespace.npcManager.link(id, target, provider, reference) },
            "Linked NPC",
        )
    }

    @Command("$commandName unlink <id>")
    @Permission(COMMAND_PERMISSION)
    fun unlink(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpcConfig(sender.sender, id) ?: return
        CommandMessages.sendResult(sender.sender, namespace.npcManager.unlink(config.id), "Unlinked NPC")
    }

    @Command("$commandName delete <id>")
    @Permission(COMMAND_PERMISSION)
    fun delete(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpcConfig(sender.sender, id) ?: return
        CommandMessages.sendResult(sender.sender, namespace.npcManager.delete(config.id), "Deleted NPC")
    }

    @Command("$commandName list")
    @Permission(COMMAND_PERMISSION)
    fun list(sender: CommandSourceStack) {
        val configs = namespace.npcRepository.findAll()
        if (configs.isEmpty()) {
            sender.sender.sendMessage(text("$PREFIX <#f59e0b>No NPCs were found."))
            return
        }
        var message = text("$PREFIX <#0ea5e9>Configured NPCs <#475569>(${configs.size})")
        configs.sortedBy { it.id }.forEach {
            message = message.appendNewline().append(
                text("   <#475569>- <#f8fafc>${it.id} <#475569>(${it.provider.type.commandName})")
            )
        }
        sender.sender.sendMessage(message)
    }

    @Command("$commandName info <id>")
    @Permission(COMMAND_PERMISSION)
    fun info(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val provider = namespace.providerRegistry.getAvailable(config.provider.type, Bukkit.getPluginManager())
        val targetStatus = runBlocking {
            config.targetServers.map { TargetResolver.resolve(it) }
        }
        val status = when {
            provider == null -> "provider unavailable"
            !provider.exists(config.provider.reference) -> "provider NPC missing"
            config.targetServers.isEmpty() -> "target missing"
            targetStatus.any { it == TargetResolution.Ambiguous } -> "target ambiguous"
            targetStatus.any { it == TargetResolution.NotFound } -> "target missing"
            else -> "complete"
        }
        sender.sender.sendMessage(
            text("$PREFIX <#0ea5e9>NPC <#f8fafc>${config.id}").appendNewline()
                .append(text("   <#94a3b8>Status<#475569>: <#e2e8f0>$status")).appendNewline()
                .append(text("   <#94a3b8>Provider<#475569>: <#e2e8f0>${config.provider.type.commandName} (${config.provider.reference})")).appendNewline()
                .append(text("   <#94a3b8>Ownership<#475569>: <#e2e8f0>${config.provider.ownership.name.lowercase()}")).appendNewline()
                .append(text("   <#94a3b8>Targets<#475569>: <#e2e8f0>${config.targetServers.joinToString()}")).appendNewline()
                .append(text("   <#94a3b8>Hologram<#475569>: <#e2e8f0>${if (config.hologram.enabled) "enabled" else "disabled"}")).appendNewline()
                .append(text("   <#94a3b8>Actions<#475569>: <#e2e8f0>${config.actions.size}"))
        )
    }

    @Command("$commandName reload")
    @Permission(COMMAND_PERMISSION)
    fun reload(sender: CommandSourceStack) {
        val result = runCatching {
            check(CommandMessages.reload())
            namespace.npcRepository.reload().forEach(namespace.hologramManager::createOrUpdate)
        }
        CommandMessages.send(
            sender.sender,
            if (result.isSuccess) "command.reload.success" else "command.reload.failed",
            if (result.isSuccess) "$PREFIX <#a3e635>SimpleCloud NPCs was reloaded."
            else "$PREFIX <#dc2626>SimpleCloud NPCs could not be reloaded.",
        )
    }
}
