package app.simplecloud.npc.common.command.edit

import app.simplecloud.npc.common.command.AbstractNpcCommand
import app.simplecloud.npc.common.command.COMMAND_PERMISSION
import app.simplecloud.npc.common.command.COMMAND_SYNTAX
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.text.sendError
import app.simplecloud.npc.common.text.sendInfo
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.platform.NpcCommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

class NpcHologramCommand<C : NpcCommandSender>(pluginContext: NpcPluginContext) : AbstractNpcCommand<C>(pluginContext) {

    @Command("$COMMAND_SYNTAX edit <id> hologram")
    @Permission(COMMAND_PERMISSION)
    fun info(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return
        val states = config.hologram.layouts.joinToString { "${it.joinState}:${it.lines.size}" }.ifBlank { "none" }
        val status = if (config.hologram.enabled) "enabled" else "disabled"
        sender.sendInfo(
            "Hologram {}: {}, height {}, layouts {}",
            id,
            status,
            config.hologram.startHeight,
            states,
        )
    }

    @Command("$COMMAND_SYNTAX edit <id> hologram enable")
    @Permission(COMMAND_PERMISSION)
    fun enable(sender: C, @Argument("id", suggestions = "npcIds") id: String) = setEnabled(sender, id, true)

    @Command("$COMMAND_SYNTAX edit <id> hologram disable")
    @Permission(COMMAND_PERMISSION)
    fun disable(sender: C, @Argument("id", suggestions = "npcIds") id: String) = setEnabled(sender, id, false)

    @Command("$COMMAND_SYNTAX edit <id> hologram height <height>")
    @Permission(COMMAND_PERMISSION)
    fun height(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("height") height: Double,
    ) {
        val config = findNpc(sender, id) ?: return
        val range = NpcConfig.HologramConfigurationRoot.START_HEIGHT_RANGE
        if (height !in range) {
            sender.sendError("Hologram height must be between {} and {}.", range.start, range.endInclusive)
            return
        }
        config.hologram.startHeight = height
        saveConfig(sender, config, "Set hologram height for NPC {} to {}.", id, height)
    }

    @Command("$COMMAND_SYNTAX edit <id> hologram line add <joinState> <content>")
    @Permission(COMMAND_PERMISSION)
    fun addLine(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("content", parserName = "greedyString") content: String,
    ) {
        val config = findNpc(sender, id) ?: return
        val layout = config.hologram.layoutOrCreate(joinState)
        layout.lines += HologramLine(content)

        saveConfig(sender, config, "Added hologram line {} for NPC {}.", layout.lines.size, id)
    }

    @Command("$COMMAND_SYNTAX edit <id> hologram line set <joinState> <line> <content>")
    @Permission(COMMAND_PERMISSION)
    fun setLine(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("line") line: Int,
        @Argument("content", parserName = "greedyString") content: String,
    ) {
        val config = findNpc(sender, id) ?: return
        val layout = findLine(sender, config, joinState, line) ?: return

        layout.lines[line - 1] = layout.lines[line - 1].copy(text = content)
        saveConfig(sender, config, "Set hologram line {} for NPC {}.", line, id)
    }

    @Command("$COMMAND_SYNTAX edit <id> hologram line remove <joinState> <line>")
    @Permission(COMMAND_PERMISSION)
    fun removeLine(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("joinState", suggestions = "joinStates") joinState: String,
        @Argument("line") line: Int,
    ) {
        val config = findNpc(sender, id) ?: return
        val layout = findLine(sender, config, joinState, line) ?: return

        layout.lines.removeAt(line - 1)
        saveConfig(sender, config, "Removed hologram line {} from NPC {}.", line, id)
    }

    private fun findLine(sender: C, config: NpcConfig, joinState: String, line: Int): NpcConfig.HologramLayout? =
        config.hologram.findLayout(joinState)?.takeIf { line in 1..it.lines.size } ?: run {
            sender.sendError("Hologram line {} does not exist.", line)
            null
        }

    private fun setEnabled(sender: C, id: String, enabled: Boolean) {
        val config = findNpc(sender, id) ?: return
        config.hologram.enabled = enabled
        val state = if (enabled) "enabled" else "disabled"

        saveConfig(sender, config, "Hologram for NPC {} was {}.", id, state)
    }
}
