package app.simplecloud.npc.plugin.paper.command.edit

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.COMMAND_PERMISSION
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.hologram.config.HologramConfiguration
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.plugin.api.shared.extension.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

class NpcHologramCommand(namespace: NpcNamespace) : AbstractNpcCommand(namespace) {

    @Command("$commandName edit <id> hologram")
    @Permission(COMMAND_PERMISSION)
    fun info(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val states = config.hologram.layouts.joinToString { "${it.joinState}:${it.lines.size}" }.ifBlank { "none" }
        sender.sender.sendMessage(
            text("$PREFIX <#0ea5e9>Hologram <#f8fafc>$id<#475569>: <#e2e8f0>${if (config.hologram.enabled) "enabled" else "disabled"}, height ${config.hologram.startHeight}, layouts $states")
        )
    }

    @Command("$commandName edit <id> hologram enable")
    @Permission(COMMAND_PERMISSION)
    fun enable(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        setEnabled(sender, id, true)
    }

    @Command("$commandName edit <id> hologram disable")
    @Permission(COMMAND_PERMISSION)
    fun disable(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        setEnabled(sender, id, false)
    }

    @Command("$commandName edit <id> hologram height <height>")
    @Permission(COMMAND_PERMISSION)
    fun height(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("height") height: Double,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        if (height !in 0.0..10.0) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Hologram height must be between 0 and 10."))
            return
        }
        config.hologram.startHeight = height
        saveConfig(sender.sender, config, "Set hologram height for NPC <#f8fafc>$id <#a3e635>to <#f8fafc>$height<#a3e635>.")
    }

    @Command("$commandName edit <id> hologram line add <joinState> <content>")
    @Permission(COMMAND_PERMISSION)
    fun addLine(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("content", parserName = "greedyString") content: String,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val layout = layout(config, joinState)
        layout.lines += HologramConfiguration(content)
        saveConfig(sender.sender, config, "Added hologram line <#f8fafc>${layout.lines.size} <#a3e635>for NPC <#f8fafc>$id<#a3e635>.")
    }

    @Command("$commandName edit <id> hologram line set <joinState> <line> <content>")
    @Permission(COMMAND_PERMISSION)
    fun setLine(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("line") line: Int,
        @Argument("content", parserName = "greedyString") content: String,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val layout = config.hologram.layouts.firstOrNull { it.joinState.equals(joinState, true) }
        if (layout == null || line !in 1..layout.lines.size) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Hologram line <#f8fafc>$line <#dc2626>does not exist."))
            return
        }
        layout.lines[line - 1] = layout.lines[line - 1].copy(text = content)
        saveConfig(sender.sender, config, "Set hologram line <#f8fafc>$line <#a3e635>for NPC <#f8fafc>$id<#a3e635>.")
    }

    @Command("$commandName edit <id> hologram line remove <joinState> <line>")
    @Permission(COMMAND_PERMISSION)
    fun removeLine(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("line") line: Int,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val layout = config.hologram.layouts.firstOrNull { it.joinState.equals(joinState, true) }
        if (layout == null || line !in 1..layout.lines.size) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Hologram line <#f8fafc>$line <#dc2626>does not exist."))
            return
        }
        layout.lines.removeAt(line - 1)
        saveConfig(sender.sender, config, "Removed hologram line <#f8fafc>$line <#a3e635>from NPC <#f8fafc>$id<#a3e635>.")
    }

    private fun setEnabled(sender: CommandSourceStack, id: String, enabled: Boolean) {
        val config = findNpcConfig(sender.sender, id) ?: return
        config.hologram.enabled = enabled
        val state = if (enabled) "enabled" else "disabled"
        saveConfig(sender.sender, config, "Hologram for NPC <#f8fafc>$id <#a3e635>was $state.")
    }

    private fun layout(config: NpcConfig, joinState: String): NpcConfig.HologramLayout {
        val state = joinState.lowercase()
        return config.hologram.layouts.firstOrNull { it.joinState.equals(state, true) }
            ?: NpcConfig.HologramLayout(state).also(config.hologram.layouts::add)
    }
}
