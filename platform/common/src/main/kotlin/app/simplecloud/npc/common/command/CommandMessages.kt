package app.simplecloud.npc.common.command

import app.simplecloud.npc.common.manager.NpcFailure
import app.simplecloud.npc.common.manager.NpcOperationResult
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.common.text.PlayerMessages
import app.simplecloud.npc.common.text.substitute
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.ActionFields
import app.simplecloud.npc.core.platform.NpcCommandSender
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path
import java.util.logging.Level

enum class NpcSuccessMessage(val path: String, val verb: String) {
    CREATED("command.npc.create.success", "Created"),
    DELETED("command.npc.delete.success", "Deleted"),
    UPDATED("command.npc.update.success", "Updated"),
    LINKED("command.npc.link.success", "Linked"),
    UNLINKED("command.npc.unlink.success", "Unlinked"),
    DUPLICATED("command.npc.duplicate.success", "Duplicated"),
}

object CommandMessages {
    val helpTopics = listOf("create", "link", "edit", "target", "hologram", "action", "pushback", "admin", "inventory")

    private const val CMD = "/$COMMAND_LABEL"

    @Volatile
    private var messagesPath: Path? = null

    private class Loaded(val root: ConfigurationNode, val variables: Map<String, String>)

    @Volatile
    private var loaded: Loaded? = null

    fun resourcePaths(): Set<String> = buildSet {
        addAll(FIXED_PATHS)
        addAll(PlayerMessages.PATHS)
        NpcSuccessMessage.entries.forEach { add(it.path) }
        NpcFailure.entries.forEach { add(failure(it).first) }
    }

    private val FIXED_PATHS = setOf(
        "command.help.title",
        "command.help.topic-title",
        "command.help.unknown",
        "command.reload.success",
        "command.reload.failed",
    )

    fun initialize(path: Path) {
        messagesPath = path
        reload()
    }

    fun reload(): Boolean {
        val path = messagesPath ?: return false

        return runCatching {
            val root = YamlConfigurationLoader.builder().path(path).build().load()
            val variables = root.node("variables").childrenMap().mapNotNull { (key, node) ->
                node.string?.let { key.toString() to it }
            }.toMap()
            loaded = Loaded(root, variables)
        }.onFailure { NpcLog.logger.log(Level.WARNING, "Could not load messages.yml", it) }.isSuccess
    }

    fun send(
        sender: NpcCommandSender,
        path: String,
        fallback: String,
        replacements: Map<String, Any?> = emptyMap(),
    ) {
        sender.sendMessage(Msg.miniMessage.deserialize(render(path, fallback, replacements)))
    }

    private fun render(path: String, fallback: String, replacements: Map<String, Any?> = emptyMap()): String {
        val current = loaded
        val configured = current?.root?.node(*path.split('.').toTypedArray())?.string ?: fallback

        val escaped = replacements.mapValues { Msg.miniMessage.escapeTags(it.value?.toString().orEmpty()) }

        return configured.substitute((current?.variables.orEmpty() + escaped).mapKeys { "<${it.key}>" })
    }

    fun sendResult(
        sender: NpcCommandSender,
        result: NpcOperationResult,
        success: NpcSuccessMessage = NpcSuccessMessage.UPDATED,
    ) {
        when (result) {
            is NpcOperationResult.Success -> send(
                sender,
                success.path,
                "${Msg.successPrefix()}${success.verb} NPC ${Msg.ACCENT}<id>${Msg.SUCCESS}.",
                mapOf("id" to result.config.id),
            )

            is NpcOperationResult.Failure -> sender.sendMessage(Msg.miniMessage.deserialize(failureMessage(result)))
        }
    }

    fun sendHelp(sender: NpcCommandSender, topic: String? = null) {
        val normalizedTopic = topic?.lowercase()
        val fallbackLines = helpPage(normalizedTopic) ?: run {
            send(
                sender,
                "command.help.unknown",
                "${Msg.errorPrefix()}Unknown help topic. ${Msg.MUTED}Try: ${Msg.ACCENT}<topics>",
                mapOf("topics" to helpTopics.joinToString()),
            )
            return
        }

        val page = normalizedTopic ?: "main"
        val title = if (normalizedTopic == null) {
            render("command.help.title", "${Msg.infoPrefix()}Commands")
        } else {
            render(
                "command.help.topic-title",
                "${Msg.infoPrefix()}Help ${Msg.SUBTLE}- ${Msg.ACCENT}<topic>",
                mapOf("topic" to normalizedTopic),
            )
        }

        val lines = listOf(title) + renderLines("command.help.pages.$page", fallbackLines)

        sender.sendMessage(Msg.miniMessage.deserialize(lines.joinToString("\n")))
    }

    private fun renderLines(path: String, fallback: List<String>): List<String> {
        val current = loaded
        val configured = current?.root?.node(*path.split('.').toTypedArray())
            ?.childrenList()
            ?.mapNotNull(ConfigurationNode::getString)
            .orEmpty()
            .ifEmpty { fallback }

        val variables = current?.variables.orEmpty().mapKeys { "<${it.key}>" }

        return configured.map { it.substitute(variables) }
    }

    private fun helpPage(topic: String?): List<String>? = when (topic) {
        null -> listOf(
            clickable("$CMD create", "<#38bdf8>$CMD create <#475569>- <#94a3b8>Create an NPC"),
            clickable(
                "$CMD link ",
                "<#38bdf8>$CMD link <#475569>- <#94a3b8>Attach an existing Citizens/FancyNpcs/ZNPCsPlus NPC",
            ),
            clickable(
                "$CMD edit",
                "<#38bdf8>$CMD edit <#f8fafc>[id] <#475569>- <#94a3b8>Edit an NPC (or the one you're looking at)",
            ),
            clickable("$CMD list", "<#38bdf8>$CMD list <#475569>- <#94a3b8>List all NPCs"),
            clickable(
                "$CMD info",
                "<#38bdf8>$CMD info <#f8fafc>[id] <#475569>- <#94a3b8>View details (or the one you're looking at)",
            ),
            clickable(
                "$CMD delete",
                "<#38bdf8>$CMD delete <#f8fafc>[id] <#475569>- <#94a3b8>Delete an NPC (or the one you're looking at)",
            ),
            "<#94a3b8>More: <#38bdf8>$CMD help <#f8fafc><topic>",
            "<#94a3b8>Topics: <#f8fafc>${helpTopics.joinToString()}",
        )

        "create" -> listOf(
            clickable("$CMD create ", "<#38bdf8>$CMD create <#f8fafc><id> <target>"),
            "<#94a3b8>Spawns a native NPC at your position, links it, and opens its editor.",
            "<#94a3b8>Example: <#f8fafc>$CMD create lobby Lobby",
        )

        "link" -> listOf(
            clickable("$CMD link ", "<#38bdf8>$CMD link <#f8fafc><id> <provider> <reference> <target>"),
            "<#94a3b8>Attaches hologram and click handling to an NPC you created with",
            "<#94a3b8>Citizens, FancyNpcs, ZNPCsPlus or MythicMobs. The NPC itself is never changed.",
            "<#94a3b8>Example: <#f8fafc>$CMD link lobby citizens 42 Lobby",
            clickable(
                "$CMD unlink ",
                "<#38bdf8>$CMD unlink <#f8fafc><id> <#475569>- <#94a3b8>Detach again, the NPC stays",
            ),
        )

        "edit" -> listOf(
            clickable("$CMD edit", "<#38bdf8>$CMD edit <#475569>- <#94a3b8>Edit the NPC you're looking at"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> target"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> hologram"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> action"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> pushback"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> teleport"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> skin <#f8fafc><player>"),
            "<#94a3b8>Use <#38bdf8>$CMD help <#f8fafc><topic> <#94a3b8>for details.",
        )

        "target" -> listOf(
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> target list"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> target add <target>"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> target remove <target>"),
        )

        "hologram" -> listOf(
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> hologram enable"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> hologram disable"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> hologram height <value>"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> hologram line add <state> <text>"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> hologram line set <state> <line> <text>"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> hologram line remove <state> <line>"),
        )

        "action" -> listOf(
            "<#38bdf8>$CMD edit <#f8fafc><id> action <type> <interaction> <state> [value]",
            "<#94a3b8>Types: <#f8fafc>join-target, open-inventory, play-sound,",
            "  <#f8fafc>send-message, execute-command, teleport-here, title,",
            "  <#f8fafc>subtitle, title-duration, action-bar, send-to-server, transfer-to-server",
            "<#38bdf8>$CMD edit <#f8fafc><id> action clear <interaction> <state> [field]",
            "<#94a3b8>Fields: <#f8fafc>${ActionFields.NAMES.joinToString()}",
        )

        "pushback" -> listOf(
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> pushback enable"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> pushback disable"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> pushback radius <value>"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> pushback strength <value>"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> pushback vertical <value>"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> pushback sound <sound>"),
            clickable("$CMD edit ", "<#38bdf8>$CMD edit <#f8fafc><id> pushback sound clear"),
        )

        "admin" -> listOf(
            clickable("$CMD list", "<#38bdf8>$CMD list <#475569>- <#94a3b8>List NPCs"),
            clickable(
                "$CMD info",
                "<#38bdf8>$CMD info <#f8fafc>[id] <#475569>- <#94a3b8>View details (or the one you're looking at)",
            ),
            clickable("$CMD tp ", "<#38bdf8>$CMD tp <#f8fafc><id> <#475569>- <#94a3b8>Teleport to an NPC"),
            clickable(
                "$CMD duplicate ",
                "<#38bdf8>$CMD duplicate <#f8fafc><id> <newId> <#475569>- <#94a3b8>Copy an NPC to where you stand",
            ),
            clickable("$CMD delete", "<#38bdf8>$CMD delete <#f8fafc>[id] <#475569>- <#94a3b8>Delete completely"),
            clickable("$CMD reload", "<#38bdf8>$CMD reload <#475569>- <#94a3b8>Reload configs"),
            clickable("$CMD placeholders", "<#38bdf8>$CMD placeholders <#475569>- <#94a3b8>List placeholders"),
        )

        "inventory" -> listOf(
            clickable("$CMD inventory create", "<#38bdf8>$CMD inventory create <#475569>- <#94a3b8>Create wizard"),
            clickable("$CMD inventory create ", "<#38bdf8>$CMD inventory create <#f8fafc><id> <rows>"),
            clickable(
                "$CMD inventory edit ",
                "<#38bdf8>$CMD inventory edit <#f8fafc><id> <#475569>- <#94a3b8>Visual editor",
            ),
            clickable("$CMD inventory delete ", "<#38bdf8>$CMD inventory delete <#f8fafc><id>"),
            clickable(
                "$CMD inventory open ",
                "<#38bdf8>$CMD inventory open <#f8fafc><id> [player] " +
                    "<#475569>- <#94a3b8>Open it, e.g. from a compass item",
            ),
            clickable("$CMD inventory list", "<#38bdf8>$CMD inventory list"),
            "<#94a3b8>Reference from an NPC via <#f8fafc>$CMD edit <id> action open-inventory",
        )

        else -> null
    }

    private fun clickable(suggestion: String, text: String): String {
        val escaped = suggestion.replace("'", "\\'")
        val hover = "<hover:show_text:'<#94a3b8>Click to fill in chat'>$text</hover>"

        return "<click:suggest_command:'$escaped'>$hover</click>"
    }

    private fun failure(failure: NpcFailure): Pair<String, String> = when (failure) {
        NpcFailure.INVALID_ID -> "command.npc.error.invalid-id" to
            "${Msg.errorPrefix()}NPC id ${Msg.ACCENT}<detail> ${Msg.ERROR}is invalid."

        NpcFailure.ALREADY_EXISTS -> "command.npc.error.already-exists" to
            "${Msg.errorPrefix()}NPC ${Msg.ACCENT}<detail> ${Msg.ERROR}already exists."

        NpcFailure.NOT_FOUND -> "command.npc.error.not-found" to
            "${Msg.errorPrefix()}NPC ${Msg.ACCENT}<detail> ${Msg.ERROR}was not found."

        NpcFailure.TARGET_NOT_FOUND -> "command.target.error.not-found" to
            "${Msg.errorPrefix()}Target ${Msg.ACCENT}<detail> ${Msg.ERROR}was not found."

        NpcFailure.TARGET_AMBIGUOUS -> "command.target.error.ambiguous" to
            "${Msg.errorPrefix()}Target ${Msg.ACCENT}<detail> ${Msg.ERROR}" +
                "exists as both a group and persistent server. Rename one of them."

        NpcFailure.TARGET_ALREADY_USED -> "command.target.error.already-used" to
            "${Msg.errorPrefix()}Target ${Msg.ACCENT}<detail> ${Msg.ERROR}is already used by this NPC."

        NpcFailure.TARGET_LIMIT -> "command.target.error.limit" to
            "${Msg.errorPrefix()}This NPC already has the maximum of ${Msg.ACCENT}<detail> ${Msg.ERROR}targets."

        NpcFailure.CREATE_FAILED -> "command.npc.create.failed" to
            "${Msg.errorPrefix()}Could not create the NPC${Msg.ACCENT}<detail-suffix>"

        NpcFailure.PROVIDER_UNAVAILABLE -> "command.provider.error.unavailable" to
            "${Msg.errorPrefix()}Provider ${Msg.ACCENT}<detail> ${Msg.ERROR}is unavailable."

        NpcFailure.PROVIDER_NPC_NOT_FOUND -> "command.provider.error.external-npc-missing" to
            "${Msg.errorPrefix()}Provider NPC ${Msg.ACCENT}<detail> ${Msg.ERROR}was not found."

        NpcFailure.PROVIDER_NPC_ALREADY_LINKED -> "command.provider.error.already-linked" to
            "${Msg.errorPrefix()}Provider NPC ${Msg.ACCENT}<detail> ${Msg.ERROR}is already linked."

        NpcFailure.NOT_LINKED -> "command.npc.error.not-linked" to
            "${Msg.errorPrefix()}NPC ${Msg.ACCENT}<detail> ${Msg.ERROR}is not a linked NPC, use delete."
    }

    private fun failureMessage(result: NpcOperationResult.Failure): String {
        val detail = result.detail.orEmpty()
        val (path, fallback) = failure(result.failure)
        val suffix = if (detail.isBlank()) "." else ": $detail"

        return render(path, fallback, mapOf("detail" to detail, "detail-suffix" to suffix))
    }
}
