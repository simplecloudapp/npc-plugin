package app.simplecloud.npc.plugin.paper.command.edit

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.COMMAND_PERMISSION
import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.plugin.api.shared.extension.text
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.key.Key
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

class NpcPushbackCommand(namespace: NpcNamespace) : AbstractNpcCommand(namespace) {

    @Command("$commandName edit <id> pushback")
    @Permission(COMMAND_PERMISSION)
    fun info(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpcConfig(sender.sender, id) ?: return
        val pushback = config.pushback
        sender.sender.sendMessage(
            text("$PREFIX <#0ea5e9>Pushback <#f8fafc>$id<#475569>: <#e2e8f0>${if (pushback.enabled) "enabled" else "disabled"}, radius ${pushback.radius}, strength ${pushback.strength}, vertical ${pushback.vertical}")
        )
    }

    @Command("$commandName edit <id> pushback enable")
    @Permission(COMMAND_PERMISSION)
    fun enable(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) = setEnabled(sender, id, true)

    @Command("$commandName edit <id> pushback disable")
    @Permission(COMMAND_PERMISSION)
    fun disable(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) = setEnabled(sender, id, false)

    @Command("$commandName edit <id> pushback radius <value>")
    @Permission(COMMAND_PERMISSION)
    fun radius(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String, @Argument("value") value: Double) {
        updateNumber(sender, id, "radius", value, 0.1..20.0) { it.pushback.radius = value }
    }

    @Command("$commandName edit <id> pushback strength <value>")
    @Permission(COMMAND_PERMISSION)
    fun strength(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String, @Argument("value") value: Double) {
        updateNumber(sender, id, "strength", value, 0.0..10.0) { it.pushback.strength = value }
    }

    @Command("$commandName edit <id> pushback vertical <value>")
    @Permission(COMMAND_PERMISSION)
    fun vertical(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String, @Argument("value") value: Double) {
        updateNumber(sender, id, "vertical boost", value, 0.0..5.0) { it.pushback.vertical = value }
    }

    @Command("$commandName edit <id> pushback sound <sound>")
    @Permission(COMMAND_PERMISSION)
    fun sound(
        sender: CommandSourceStack,
        @Argument("id", suggestions = "npcIds") id: String,
        @Argument("sound", suggestions = "sounds") sound: String,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        if (runCatching { Key.key(sound) }.isFailure) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Sound <#f8fafc>$sound <#dc2626>is invalid."))
            return
        }
        config.pushback.sound = sound
        saveConfig(sender.sender, config, "Set pushback sound for NPC <#f8fafc>$id <#a3e635>to <#f8fafc>$sound<#a3e635>.", false)
    }

    @Command("$commandName edit <id> pushback sound clear")
    @Permission(COMMAND_PERMISSION)
    fun clearSound(sender: CommandSourceStack, @Argument("id", suggestions = "npcIds") id: String) {
        val config = findNpcConfig(sender.sender, id) ?: return
        config.pushback.sound = null
        saveConfig(sender.sender, config, "Cleared the pushback sound for NPC <#f8fafc>$id<#a3e635>.", false)
    }

    private fun setEnabled(sender: CommandSourceStack, id: String, enabled: Boolean) {
        val config = findNpcConfig(sender.sender, id) ?: return
        config.pushback.enabled = enabled
        saveConfig(sender.sender, config, "Pushback for NPC <#f8fafc>$id <#a3e635>was ${if (enabled) "enabled" else "disabled"}.", false)
    }

    private fun updateNumber(
        sender: CommandSourceStack,
        id: String,
        property: String,
        value: Double,
        range: ClosedFloatingPointRange<Double>,
        update: (app.simplecloud.npc.shared.config.NpcConfig) -> Unit,
    ) {
        val config = findNpcConfig(sender.sender, id) ?: return
        if (value !in range) {
            sender.sender.sendMessage(text("$PREFIX <#dc2626>Pushback $property must be between ${range.start} and ${range.endInclusive}."))
            return
        }
        update(config)
        saveConfig(sender.sender, config, "Set pushback $property for NPC <#f8fafc>$id <#a3e635>to <#f8fafc>$value<#a3e635>.", false)
    }
}
