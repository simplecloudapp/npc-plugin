package app.simplecloud.npc.common.command

import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.ui.Clicks
import app.simplecloud.npc.common.manager.NpcStatus
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.cloud.CloudSnapshot
import app.simplecloud.npc.core.cloud.TargetResolution
import app.simplecloud.npc.core.cloud.TargetResolver
import app.simplecloud.npc.core.cloud.TargetType
import app.simplecloud.npc.core.cloud.groupName
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.render.Providers
import java.util.Locale

object NpcInfoPage {

    private const val TEXT = "<#e2e8f0>"
    private const val KEY = "<#38bdf8>"
    private const val OK = "<#a3e635>"

    fun render(config: NpcConfig, status: NpcStatus, snapshot: CloudSnapshot): String {
        val cmd = "/$COMMAND_LABEL"
        val id = config.id
        val badge = statusBadge(config, status, snapshot)
        val editButton = button("Edit", "$cmd edit $id", "Open the editor", run = true)
        val moveButton = button("Move here", "$cmd edit $id teleport", "Moves the NPC to where you stand", run = true)

        return buildList {
            add("${Msg.infoPrefix()}${Msg.ACCENT}${esc(id)}${nameSuffix(config)}  $badge  $editButton $moveButton")
            add(row("Target", targetsValue(config, snapshot, cmd)))
            add(row("Provider", providerValue(config)))
            add(row("Location", locationValue(config, cmd)))
            hologramLines(config).forEachIndexed { index, line -> add(row(if (index == 0) "Hologram" else "", line)) }
            actionLines(config).forEachIndexed { index, line -> add(row(if (index == 0) "Actions" else "", line)) }
            add(row("Pushback", pushbackValue(config)))
        }.joinToString("\n")
    }

    private fun nameSuffix(config: NpcConfig): String =
        NpcFormat.displayName(config).takeIf { it != config.id }?.let { " ${Msg.MUTED}(${esc(it)})" }.orEmpty()

    private fun statusBadge(config: NpcConfig, status: NpcStatus, snapshot: CloudSnapshot): String {
        val targetDerived = status == NpcStatus.TARGET_MISSING || status == NpcStatus.TARGET_AMBIGUOUS
        if (targetDerived && config.targetServers.isNotEmpty() && !snapshot.known) {
            return hover("${Msg.MUTED}● Checking…", "The cloud lists have not been read yet.")
        }

        return when (status) {
            NpcStatus.COMPLETE ->
                hover("$OK● FUNCTIONAL", "Everything is fine.")

            NpcStatus.NEEDS_RELOCATION ->
                hover(
                    "${Msg.WARNING}● Needs relocation",
                    "Its old position could not be taken over. Stand where it should appear and click Move here.",
                )

            NpcStatus.TARGET_AMBIGUOUS -> {
                val names = namesWith(config, snapshot) { it == TargetResolution.Ambiguous }
                hover(
                    "${Msg.ERROR}● Ambiguous target: ${esc(names)}",
                    "A group and a persistent server share this name. Rename one of them.",
                )
            }

            NpcStatus.TARGET_MISSING -> if (config.targetServers.isEmpty()) {
                hover(
                    "${Msg.ERROR}● No target",
                    "Clicks have nowhere to send players. " +
                        "Add one with /$COMMAND_LABEL edit ${config.id} target add <target>.",
                    escape = true,
                )
            } else {
                val names = namesWith(config, snapshot) { it == TargetResolution.NotFound }
                hover(
                    "${Msg.ERROR}● Target missing: ${esc(names)}",
                    "No group or persistent server with this name exists in the cloud.",
                )
            }
        }
    }

    private fun namesWith(
        config: NpcConfig,
        snapshot: CloudSnapshot,
        predicate: (TargetResolution) -> Boolean,
    ): String = config.targetServers.filter { predicate(TargetResolver.resolve(it, snapshot)) }.joinToString()

    private fun targetsValue(config: NpcConfig, snapshot: CloudSnapshot, cmd: String): String {
        if (config.targetServers.isEmpty()) return "${Msg.MUTED}none"
        val value = config.targetServers.joinToString("${Msg.SUBTLE}, ") { target ->
            "$TEXT${esc(target)} ${targetLive(target, snapshot)}"
        }

        return click(value, "$cmd edit ${config.id} target list", "Show the targets", run = true)
    }

    private fun targetLive(target: String, snapshot: CloudSnapshot): String {
        if (!snapshot.known) return "${Msg.SUBTLE}loading…"
        val resolution = TargetResolver.resolve(target, snapshot)
        if (resolution !is TargetResolution.Found) return "${Msg.ERROR}not found"
        val servers = snapshot.servers.filter { server ->
            server.groupName().equals(target, ignoreCase = true) ||
                server.persistentServer?.name.equals(target, ignoreCase = true)
        }
        val players = servers.sumOf { it.playerCount ?: 0 }

        return when (resolution.target.type) {
            TargetType.GROUP ->
                "${Msg.SUBTLE}group · ${Msg.MUTED}${servers.size} online · ${plural(players, "player")}"

            TargetType.PERSISTENT_SERVER -> if (servers.isEmpty()) {
                "${Msg.SUBTLE}server · ${Msg.WARNING}offline"
            } else {
                "${Msg.SUBTLE}server · ${Msg.MUTED}online · ${plural(players, "player")}"
            }
        }
    }

    private fun providerValue(config: NpcConfig): String {
        val entity = config.entity
        val name = providerName(entity.provider)

        return if (entity.providerLinked) {
            "$TEXT$name${Msg.SUBTLE}, ${Msg.MUTED}linked ${Msg.SUBTLE}(${esc(entity.providerReference.orEmpty())})"
        } else {
            "$TEXT$name"
        }
    }

    private fun providerName(key: String): String = esc(Providers.displayName(key))

    private fun locationValue(config: NpcConfig, cmd: String): String {
        if (config.entity.needsRelocation) return "${Msg.WARNING}not placed yet"
        val location = config.entity.location
        val coordinates = "${location.x.toInt()} ${location.y.toInt()} ${location.z.toInt()}"
        val value = "$TEXT${esc(location.world)}  ${Msg.MUTED}$coordinates"

        return click(value, "$cmd tp ${config.id}", "Teleport to the NPC", run = true)
    }

    private fun hologramLines(config: NpcConfig): List<String> {
        val hologram = config.hologram
        if (!hologram.enabled) return listOf("${Msg.MUTED}off")
        val layouts = hologram.layouts.filter { it.lines.isNotEmpty() }
            .sortedBy { if (NpcFormat.isDefaultJoinState(it.joinState)) 0 else 1 }
        if (layouts.isEmpty()) return listOf("${Msg.MUTED}on, but no lines yet")
        val labelWidth = layouts.maxOf { ChatPixels.width(it.joinState.lowercase()) }

        return layouts.map { layout ->
            val state = layout.joinState.lowercase()
            val preview = layout.lines.joinToString("\n") { it.shownText }
            "${Msg.MUTED}${ChatPixels.pad(esc(state), labelWidth)}  " +
                hover("$TEXT${plural(layout.lines.size, "line")}", preview, formatted = true)
        }
    }

    private fun actionLines(config: NpcConfig): List<String> {
        val byClick = NpcFormat.CLICK_ROWS.flatMap { click ->
            config.actions.filter { it.interactionType == click && !it.isEmpty() }
                .sortedBy { if (NpcFormat.isDefaultJoinState(it.joinState)) 0 else 1 }
        }
        if (byClick.isEmpty()) return listOf("${Msg.MUTED}none")
        val labelWidth = byClick.maxOf { ChatPixels.width(Clicks.short(it.interactionType)) }

        return byClick.map { action ->
            val label = Clicks.short(action.interactionType)
            val state = if (NpcFormat.isDefaultJoinState(action.joinState)) {
                ""
            } else {
                " ${Msg.SUBTLE}(${esc(action.joinState.lowercase())})"
            }
            "${Msg.MUTED}${ChatPixels.pad(label, labelWidth)}  $TEXT${describe(action)}$state"
        }
    }

    private fun describe(action: NpcConfig.ActionConfiguration): String = buildList {
        if (action.joinTarget) add("join target")
        action.sendToServer?.let { add("send to ${esc(it)}") }
        action.transferToServer?.let { add("transfer to ${esc(it)}") }
        action.openInventory?.let { add("open menu ${esc(it)}") }
        action.teleport?.let { add("teleport to ${esc(NpcFormat.position(it))}") }
        action.sendMessage?.let { add(hover("message", it, formatted = true)) }
        action.sendTitle?.let { title ->
            val text = Msg.miniMessage.stripTags(title.title.ifBlank { title.subtitle })
            add("title \"${esc(text)}\"")
        }
        action.executeCommand?.let { add("command /${esc(it)}") }
        action.playSound?.let { add("sound ${esc(it.lowercase())}") }
        action.actionBar?.let { add(hover("action bar", it, formatted = true)) }
        action.speech?.let { add(hover("speech", it, formatted = true)) }
        action.npcAnimation?.let { add("animation ${it.name.lowercase()}") }
        action.permission?.takeIf { it.isNotBlank() }?.let { add("needs ${esc(it)}") }
    }.joinToString("${Msg.SUBTLE} + $TEXT")

    private fun pushbackValue(config: NpcConfig): String {
        val pushback = config.pushback
        if (!pushback.enabled) return "${Msg.MUTED}off"
        val sound = pushback.sound?.let { " ${Msg.SUBTLE}· ${Msg.MUTED}sound ${esc(it.lowercase())}" }.orEmpty()
        val radius = "${TEXT}radius ${number(pushback.radius)}"
        val strength = "${TEXT}strength ${number(pushback.strength)}"

        return "$radius ${Msg.SUBTLE}· $strength$sound"
    }

    private val LABEL_WIDTH =
        listOf("Target", "Provider", "Location", "Hologram", "Actions", "Pushback").maxOf(ChatPixels::width)

    private fun row(label: String, value: String) = "  $KEY${ChatPixels.pad(label, LABEL_WIDTH)}  $value"

    private fun button(label: String, command: String, tip: String, run: Boolean): String =
        click("${Msg.INFO}[$label]", command, tip, run)

    private fun click(content: String, command: String, tip: String, run: Boolean): String {
        val action = if (run) "run_command" else "suggest_command"
        return "<click:$action:'${arg(command)}'>${hover(content, tip)}</click>"
    }

    private fun hover(content: String, tip: String, escape: Boolean = false, formatted: Boolean = false): String {
        val text = when {
            escape -> "${Msg.MUTED}${esc(tip)}"
            formatted -> "$TEXT$tip"
            else -> "${Msg.MUTED}$tip"
        }

        return "<hover:show_text:'${arg(text)}'>$content</hover>"
    }

    private fun arg(text: String) = text.replace("\\", "\\\\").replace("'", "\\'")

    private fun esc(text: String) = Msg.miniMessage.escapeTags(text)

    private fun plural(count: Int, word: String) = "$count $word${if (count == 1) "" else "s"}"

    private fun number(value: Double) = String.format(Locale.ROOT, "%.1f", value)
}
