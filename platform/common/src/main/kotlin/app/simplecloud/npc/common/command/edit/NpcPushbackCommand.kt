package app.simplecloud.npc.common.command.edit

import app.simplecloud.npc.common.command.AbstractNpcCommand
import app.simplecloud.npc.common.command.COMMAND_PERMISSION
import app.simplecloud.npc.common.command.COMMAND_SYNTAX
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.text.sendError
import app.simplecloud.npc.common.text.sendInfo
import app.simplecloud.npc.common.utils.InputChecks
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcCommandSender
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

class NpcPushbackCommand<C : NpcCommandSender>(pluginContext: NpcPluginContext) : AbstractNpcCommand<C>(pluginContext) {

    @Command("$COMMAND_SYNTAX edit <id> pushback")
    @Permission(COMMAND_PERMISSION)
    fun info(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return
        val pushback = config.pushback
        val status = if (pushback.enabled) "enabled" else "disabled"

        sender.sendInfo(
            "Pushback {}: {}, radius {}, strength {}, vertical {}",
            id,
            status,
            pushback.radius,
            pushback.strength,
            pushback.vertical,
        )
    }

    @Command("$COMMAND_SYNTAX edit <id> pushback enable")
    @Permission(COMMAND_PERMISSION)
    fun enable(sender: C, @Argument("id", suggestions = "npcIds") id: String) = setEnabled(sender, id, true)

    @Command("$COMMAND_SYNTAX edit <id> pushback disable")
    @Permission(COMMAND_PERMISSION)
    fun disable(sender: C, @Argument("id", suggestions = "npcIds") id: String) = setEnabled(sender, id, false)

    @Command("$COMMAND_SYNTAX edit <id> pushback radius <value>")
    @Permission(COMMAND_PERMISSION)
    fun radius(sender: C, @Argument("id", suggestions = "npcIds") id: String, @Argument("value") value: Double) =
        updateNumber(
            sender,
            id,
            "radius",
            value,
            NpcConfig.PushbackConfiguration.RADIUS_RANGE,
        ) { it.pushback.radius = value }

    @Command("$COMMAND_SYNTAX edit <id> pushback strength <value>")
    @Permission(COMMAND_PERMISSION)
    fun strength(sender: C, @Argument("id", suggestions = "npcIds") id: String, @Argument("value") value: Double) =
        updateNumber(
            sender,
            id,
            "strength",
            value,
            NpcConfig.PushbackConfiguration.STRENGTH_RANGE,
        ) { it.pushback.strength = value }

    @Command("$COMMAND_SYNTAX edit <id> pushback vertical <value>")
    @Permission(COMMAND_PERMISSION)
    fun vertical(sender: C, @Argument("id", suggestions = "npcIds") id: String, @Argument("value") value: Double) =
        updateNumber(
            sender,
            id,
            "vertical boost",
            value,
            NpcConfig.PushbackConfiguration.VERTICAL_RANGE,
        ) { it.pushback.vertical = value }

    @Command("$COMMAND_SYNTAX edit <id> pushback sound <sound>")
    @Permission(COMMAND_PERMISSION)
    fun sound(
        sender: C,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("sound", parserName = "greedyString") sound: String,
    ) {
        val config = findNpc(sender, id) ?: return
        if (!InputChecks.isValidSound(sound)) {
            sender.sendError("Sound {} is invalid.", sound)
            return
        }

        config.pushback.sound = sound
        saveConfig(sender, config, "Set pushback sound for NPC {} to {}.", id, sound, refreshHologram = false)
    }

    @Command("$COMMAND_SYNTAX edit <id> pushback sound clear")
    @Permission(COMMAND_PERMISSION)
    fun clearSound(sender: C, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpc(sender, id) ?: return
        config.pushback.sound = null

        saveConfig(sender, config, "Cleared the pushback sound for NPC {}.", id, refreshHologram = false)
    }

    private fun setEnabled(sender: C, id: String, enabled: Boolean) {
        val config = findNpc(sender, id) ?: return
        config.pushback.enabled = enabled
        val state = if (enabled) "enabled" else "disabled"

        saveConfig(sender, config, "Pushback for NPC {} was {}.", id, state, refreshHologram = false)
    }

    private fun updateNumber(
        sender: C,
        id: String,
        property: String,
        value: Double,
        range: ClosedFloatingPointRange<Double>,
        update: (NpcConfig) -> Unit,
    ) {
        val config = findNpc(sender, id) ?: return
        if (value !in range) {
            sender.sendError("Pushback {} must be between {} and {}.", property, range.start, range.endInclusive)
            return
        }

        update(config)
        saveConfig(sender, config, "Set pushback {} for NPC {} to {}.", property, id, value, refreshHologram = false)
    }
}
