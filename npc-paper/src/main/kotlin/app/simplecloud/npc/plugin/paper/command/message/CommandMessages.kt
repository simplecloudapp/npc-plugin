package app.simplecloud.npc.plugin.paper.command.message

import app.simplecloud.npc.plugin.paper.command.PREFIX
import app.simplecloud.npc.shared.manager.NpcFailure
import app.simplecloud.npc.shared.manager.NpcOperationResult
import app.simplecloud.plugin.api.shared.extension.text
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

object CommandMessages {
    val helpTopics = listOf("create", "link", "edit", "target", "hologram", "action", "pushback", "admin")

    private var messagesPath: Path? = null
    private var root: ConfigurationNode? = null
    private var variables: Map<String, String> = emptyMap()

    fun initialize(plugin: JavaPlugin) {
        plugin.saveResource("messages.yml", false)
        messagesPath = plugin.dataFolder.toPath().resolve("messages.yml")
        check(reload()) { "Could not load messages.yml" }
    }

    fun reload(): Boolean {
        val path = messagesPath ?: return false
        return runCatching {
            root = YamlConfigurationLoader.builder().path(path).build().load()
            variables = root?.node("variables")?.childrenMap()?.mapNotNull { (key, node) ->
                node.string?.let { key.toString() to it }
            }?.toMap().orEmpty()
        }.isSuccess
    }

    fun send(
        sender: CommandSender,
        path: String,
        fallback: String,
        replacements: Map<String, Any?> = emptyMap(),
    ) {
        sender.sendMessage(text(render(path, fallback, replacements)))
    }

    fun render(path: String, fallback: String, replacements: Map<String, Any?> = emptyMap()): String {
        val configured = root?.node(*path.split('.').toTypedArray())?.string ?: fallback
        return (variables + replacements.mapValues { it.value?.toString().orEmpty() })
            .entries
            .fold(configured) { message, (key, value) -> message.replace("<$key>", value) }
    }

    fun sendResult(sender: CommandSender, result: NpcOperationResult, success: String) {
        when (result) {
            is NpcOperationResult.Success -> {
                val path = when (success.substringBefore(' ').lowercase()) {
                    "created" -> "command.npc.create.success"
                    "linked" -> "command.npc.link.success"
                    "unlinked" -> "command.npc.unlink.success"
                    "deleted" -> "command.npc.delete.success"
                    else -> "command.npc.update.success"
                }
                send(
                    sender,
                    path,
                    "$PREFIX <#a3e635>$success <#f8fafc><id><#a3e635>.",
                    mapOf("id" to result.config.id),
                )
            }
            is NpcOperationResult.Failure -> sender.sendMessage(text(failureMessage(result)))
        }
    }

    fun sendHelp(sender: CommandSender, topic: String? = null) {
        val normalizedTopic = topic?.lowercase()
        val fallbackLines = helpPage(normalizedTopic)
        if (fallbackLines == null) {
            send(
                sender,
                "command.help.unknown",
                "$PREFIX <#dc2626>Unknown help topic. <#94a3b8>Try: <#f8fafc>${helpTopics.joinToString()}",
            )
            return
        }

        val page = normalizedTopic ?: "main"
        val title = if (normalizedTopic == null) {
            render("command.help.title", "$PREFIX <#0ea5e9>Commands")
        } else {
            render(
                "command.help.topic-title",
                "$PREFIX <#0ea5e9>Help <#475569>— <#f8fafc><topic>",
                mapOf("topic" to normalizedTopic),
            )
        }
        var message = text(title)
        renderLines("command.help.pages.$page", fallbackLines).forEach { line ->
            message = message.appendNewline().append(text(line))
        }
        sender.sendMessage(message)
    }

    private fun renderLines(path: String, fallback: List<String>): List<String> {
        val configured = root?.node(*path.split('.').toTypedArray())
            ?.childrenList()
            ?.mapNotNull(ConfigurationNode::getString)
            .orEmpty()
            .ifEmpty { fallback }
        return configured.map { line ->
            variables.entries.fold(line) { message, (key, value) -> message.replace("<$key>", value) }
        }
    }

    private fun helpPage(topic: String?): List<String>? = when (topic) {
        null -> listOf(
            "<#38bdf8>/scnpcs create <#f8fafc><id> <target> <#475569>— <#94a3b8>Create an NPC",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> <#475569>— <#94a3b8>Edit an NPC",
            "<#38bdf8>/scnpcs list <#475569>— <#94a3b8>List all NPCs",
            "<#38bdf8>/scnpcs info <#f8fafc><id> <#475569>— <#94a3b8>View details",
            "<#38bdf8>/scnpcs delete <#f8fafc><id> <#475569>— <#94a3b8>Delete an NPC",
            "<#94a3b8>More: <#38bdf8>/scnpcs help <#f8fafc><topic>",
            "<#94a3b8>Topics: <#f8fafc>create, link, edit, target",
            "<#f8fafc>hologram, action, pushback, admin",
        )
        "create" -> listOf(
            "<#38bdf8>/scnpcs create <#f8fafc><id> <target> [provider]",
            "<#94a3b8>Creates the provider NPC at your position and links it.",
            "<#94a3b8>Example: <#f8fafc>/scnpcs create lobby Lobby",
        )
        "link" -> listOf(
            "<#38bdf8>/scnpcs link <#f8fafc><id> <provider>",
            "  <#f8fafc><reference> <target>",
            "<#94a3b8>Links an NPC that already exists in a provider.",
            "<#94a3b8>Example: <#f8fafc>/scnpcs link lobby citizens 12 Lobby",
        )
        "edit" -> listOf(
            "<#38bdf8>/scnpcs edit <#f8fafc><id> target",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> hologram",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> action",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> pushback",
            "<#94a3b8>Use <#38bdf8>/scnpcs help <#f8fafc><topic> <#94a3b8>for details.",
        )
        "target" -> listOf(
            "<#38bdf8>/scnpcs edit <#f8fafc><id> target list",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> target add <target>",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> target remove <target>",
        )
        "hologram" -> listOf(
            "<#38bdf8>/scnpcs edit <#f8fafc><id> hologram enable",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> hologram disable",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> hologram height <value>",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> hologram line add",
            "  <#f8fafc><state> <text>",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> hologram line set",
            "  <#f8fafc><state> <line> <text>",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> hologram line remove",
            "  <#f8fafc><state> <line>",
        )
        "action" -> listOf(
            "<#38bdf8>/scnpcs edit <#f8fafc><id> action <type>",
            "  <#f8fafc><interaction> <state> [value]",
            "<#94a3b8>Types: <#f8fafc>join-target, play-sound, send-message,",
            "  <#f8fafc>execute-command, teleport-here, title, subtitle,",
            "  <#f8fafc>send-title, title-duration,",
            "  <#f8fafc>send-to-server, transfer-to-server",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> action clear",
            "  <#f8fafc><interaction> <state> [type]",
        )
        "pushback" -> listOf(
            "<#38bdf8>/scnpcs edit <#f8fafc><id> pushback enable",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> pushback disable",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> pushback radius <value>",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> pushback strength <value>",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> pushback vertical <value>",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> pushback sound <sound>",
            "<#38bdf8>/scnpcs edit <#f8fafc><id> pushback sound clear",
        )
        "admin" -> listOf(
            "<#38bdf8>/scnpcs list <#475569>— <#94a3b8>List NPCs",
            "<#38bdf8>/scnpcs info <#f8fafc><id> <#475569>— <#94a3b8>View details",
            "<#38bdf8>/scnpcs unlink <#f8fafc><id> <#475569>— <#94a3b8>Keep provider NPC",
            "<#38bdf8>/scnpcs delete <#f8fafc><id> <#475569>— <#94a3b8>Delete completely",
            "<#38bdf8>/scnpcs reload <#475569>— <#94a3b8>Reload configs",
            "<#38bdf8>/scnpcs placeholders <#475569>— <#94a3b8>List placeholders",
        )
        else -> null
    }

    private fun failureMessage(result: NpcOperationResult.Failure): String {
        val detail = result.detail.orEmpty()
        val (path, fallback) = when (result.failure) {
            NpcFailure.INVALID_ID -> "command.npc.error.invalid-id" to "$PREFIX <#dc2626>NPC id <#f8fafc><detail> <#dc2626>is invalid."
            NpcFailure.ALREADY_EXISTS -> "command.npc.error.already-exists" to "$PREFIX <#dc2626>NPC <#f8fafc><detail> <#dc2626>already exists."
            NpcFailure.NOT_FOUND -> "command.npc.error.not-found" to "$PREFIX <#dc2626>NPC <#f8fafc><detail> <#dc2626>was not found."
            NpcFailure.TARGET_NOT_FOUND -> "command.target.error.not-found" to "$PREFIX <#dc2626>Target <#f8fafc><detail> <#dc2626>was not found."
            NpcFailure.TARGET_AMBIGUOUS -> "command.target.error.ambiguous" to "$PREFIX <#dc2626>Target <#f8fafc><detail> <#dc2626>exists as both a group and persistent server. Rename one of them."
            NpcFailure.PROVIDER_REQUIRED -> "command.provider.error.required" to "$PREFIX <#dc2626>More than one creation provider is installed; specify one."
            NpcFailure.CREATION_PROVIDER_UNAVAILABLE -> "command.provider.error.creation-unavailable" to "$PREFIX <#dc2626>Install Citizens, FancyNPCs, or ZNPCsPlus to create NPCs."
            NpcFailure.PROVIDER_UNAVAILABLE -> "command.provider.error.unavailable" to "$PREFIX <#dc2626>Provider <#f8fafc><detail> <#dc2626>is unavailable."
            NpcFailure.PROVIDER_NPC_NOT_FOUND -> "command.provider.error.external-npc-missing" to "$PREFIX <#dc2626>Provider NPC <#f8fafc><detail> <#dc2626>was not found."
            NpcFailure.PROVIDER_NPC_ALREADY_LINKED -> "command.provider.error.already-linked" to "$PREFIX <#dc2626>Provider NPC <#f8fafc><detail> <#dc2626>is already linked."
            NpcFailure.CREATE_FAILED -> "command.npc.create.failed" to "$PREFIX <#dc2626>Could not create the NPC<#f8fafc><detail-suffix>"
            NpcFailure.DELETE_FAILED -> "command.npc.delete.failed" to "$PREFIX <#dc2626>Could not delete NPC <#f8fafc><detail><#dc2626>."
        }
        val suffix = if (detail.isBlank()) "." else ": $detail"
        return render(path, fallback, mapOf("detail" to detail, "detail-suffix" to suffix))
    }
}
