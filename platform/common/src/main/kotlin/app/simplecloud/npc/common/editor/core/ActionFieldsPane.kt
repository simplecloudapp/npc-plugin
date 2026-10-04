package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.PromptPlaceholder
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.common.editor.PromptTokens
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.ui.Stepper
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.editor.ui.after
import app.simplecloud.npc.common.utils.InputChecks
import app.simplecloud.npc.core.config.ActionFields
import app.simplecloud.npc.core.config.NpcAnimation
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.config.NpcConfig.ActionConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer
import net.kyori.adventure.text.Component

object ActionFieldsPane {
    const val SIZE = 54
    const val HEADER_SLOT = 4
    const val COPY_SLOT = 48
    const val WIPE_SLOT = 50

    private const val JOIN_TARGET_SLOT = 10
    private const val MENU_SLOT = 12
    private const val SERVER_SLOT = 14
    private const val TELEPORT_SLOT = 16
    private const val TRANSFER_SLOT = 19
    private const val MESSAGE_SLOT = 21
    private const val TITLE_SLOT = 23
    private const val SOUND_SLOT = 25
    private const val COMMAND_SLOT = 28
    private const val ACTION_BAR_SLOT = 30
    private const val SCOPED_FIRST_SLOT = 32
    private const val SCOPED_SECOND_SLOT = 34
    private const val COOLDOWN_SLOT = 39
    private const val PERMISSION_SLOT = 41

    private const val SILENT = "silent"
    private val ANIMATIONS: List<NpcAnimation?> = listOf(null) + NpcAnimation.entries

    private val COOLDOWN = Stepper(
        label = "Cooldown",
        material = "CLOCK",
        range = 0.0..60.0,
        step = 0.5,
        default = NpcConfig.DEFAULT_COOLDOWN_MILLIS / 1000.0,
        decimals = 1,
        unit = "s",
        hints = listOf("<hnt>Repeat clicks inside it are ignored.", "<hnt>0 turns it off."),
    )

    enum class Scope { NPC, MENU }

    class Port(
        val scope: Scope,
        val player: NpcPlayer,
        val action: ActionConfiguration,
        val textPrompts: TextPrompts,
        val edit: ((ActionConfiguration) -> Unit) -> Unit,
        val toggleJoinTarget: () -> Unit,
        val prompt: (Prompt) -> Unit,
        val rerender: () -> Unit,
        val pickMenu: () -> Unit,
        val pickServer: () -> Unit,
        val pickSound: () -> Unit,
        val openTitle: () -> Unit,
    )

    fun fieldCount(scope: Scope): Int = ActionFields.NAMES.size + when (scope) {
        Scope.NPC -> ActionFields.NPC_ONLY.size
        Scope.MENU -> ActionFields.MENU_ONLY.size
    }

    fun fill(pane: Pane, port: Port) {
        val action = port.action

        fun clearOnDrop(click: MenuClick, isSet: Boolean, clear: (ActionConfiguration) -> Unit) {
            if (click == MenuClick.DROP && isSet) port.edit(clear)
        }

        fun chat(
            title: String,
            instruction: String,
            current: String?,
            args: List<Any?> = emptyList(),
            format: PromptFormat = PromptFormat.MINI_MESSAGE,
            placeholders: List<PromptPlaceholder> = PromptTokens.PLAYER,
            clear: ((ActionConfiguration) -> Unit)? = null,
            validate: (String) -> PromptResult.Rejected? = PromptResult::rejectInvalidMiniMessage,
            apply: (ActionConfiguration, String) -> Unit,
        ) {
            port.prompt(
                Prompt(
                    title = title,
                    instruction = instruction,
                    instructionArgs = args,
                    current = current,
                    format = format,
                    placeholders = placeholders,
                    onClear = clear?.let { { port.edit(it) } },
                    onCancel = port.rerender,
                    onSubmit = { input ->
                        val text = input.trim()
                        validate(text)?.let { return@Prompt it }
                        port.edit { apply(it, text) }
                        PromptResult.Accepted
                    },
                ),
            )
        }

        val joinTargetHint = when (port.scope) {
            Scope.MENU -> "<hnt>Joins the opening NPC's target."
            Scope.NPC -> "<hnt>Joins the NPC's target."
        }
        pane.toggle(
            JOIN_TARGET_SLOT,
            "Join Target",
            action.joinTarget,
            extraLore = listOf(joinTargetHint),
            material = "NETHER_STAR",
        ) { port.toggleJoinTarget() }

        pane.on(
            MENU_SLOT,
            Ui.field(
                "CHEST",
                "Open Menu",
                action.openInventory?.let { "<bd>Menu <val>${NpcFormat.plain(it)}" },
                "<key>Left <hnt>Pick a menu",
            ),
        ) { click ->
            if (click == MenuClick.LEFT) port.pickMenu()
            clearOnDrop(click, action.openInventory != null) { it.openInventory = null }
        }

        pane.on(
            SERVER_SLOT,
            Ui.field(
                "COMPASS",
                "Send To Server",
                action.sendToServer?.let { "<bd>Server <val>${NpcFormat.plain(it)}" },
                "<key>Left <hnt>Pick a server",
            ),
        ) { click ->
            if (click == MenuClick.LEFT) port.pickServer()
            clearOnDrop(click, action.sendToServer != null) { it.sendToServer = null }
        }

        pane.on(
            TELEPORT_SLOT,
            Ui.field(
                "RECOVERY_COMPASS",
                "Teleport",
                action.teleport?.let { "<bd>To <val>${NpcFormat.position(it)}" },
                "<key>Shift+Right <hnt>Capture my position",
            ),
        ) { click ->
            if (click == MenuClick.SHIFT_RIGHT) {
                val at = port.player.location()
                port.edit { it.teleport = at }
            }
            clearOnDrop(click, action.teleport != null) { it.teleport = null }
        }

        pane.on(
            TRANSFER_SLOT,
            Ui.field(
                "ENDER_EYE",
                "Transfer To Host",
                action.transferToServer?.let { "<bd>Host <val>${NpcFormat.plain(it)}" },
                "<key>Left <hnt>Type host:port in chat",
            ),
        ) { click ->
            if (click == MenuClick.LEFT) chat(
                "Transfer To Host",
                "Type the host to transfer to in chat, as host:port.",
                action.transferToServer,
                format = PromptFormat.PLAIN,
                placeholders = emptyList(),
                clear = { it.transferToServer = null },
                validate = {
                    if (InputChecks.isValidHostPort(it)) {
                        null
                    } else {
                        PromptResult.Rejected(Component.text("Expected host:port."))
                    }
                },
            ) { a, text -> a.transferToServer = text }
            clearOnDrop(click, action.transferToServer != null) { it.transferToServer = null }
        }

        val messageLines = NpcFormat.messageLines(action.sendMessage)
        pane.on(
            MESSAGE_SLOT,
            if (messageLines.isEmpty()) {
                Ui.field("WRITABLE_BOOK", "Chat Message", null, "<key>Left <hnt>Type it in chat")
            } else {
                Ui.item(
                    "WRITABLE_BOOK",
                    "<ttl>Chat Message",
                    listOfNotNull(
                        "<bd><val>${messageLines.size} <bd>lines set",
                        "<val>${Ui.quote(messageLines.first())}",
                        "<hnt>+${messageLines.size - 1} more".takeIf { messageLines.size > 1 },
                        "",
                        "<key>Left <hnt>Type it in chat",
                        "<key>Q <hnt>Clear",
                    ),
                    glowing = true,
                )
            },
        ) { click ->
            if (click == MenuClick.LEFT) chat(
                "Chat Message",
                "Type the chat message, {} starts a new line.",
                action.sendMessage?.let { NpcFormat.messageLines(it).joinToString("<newline>") },
                args = listOf("<newline>"),
                clear = { it.sendMessage = null },
            ) { a, text -> a.sendMessage = text }
            clearOnDrop(click, action.sendMessage != null) { it.sendMessage = null }
        }

        pane.on(
            TITLE_SLOT,
            action.sendTitle?.let { title ->
                Ui.item(
                    "PAINTING",
                    "<ttl>On-Screen Title",
                    listOf(
                        "<bd>Title <val>${Ui.quote(title.title)}",
                        "<bd>Subtitle <val>${Ui.quote(title.subtitle)}",
                        "<bd>Pacing ${NpcFormat.pacing(title)}",
                        "",
                        "<key>Left <info>Open title editor",
                        "<key>Q <err>Remove title entirely",
                    ),
                    glowing = true,
                )
            } ?: Ui.field("PAINTING", "On-Screen Title", null, "<key>Left <info>Open title editor"),
        ) { click ->
            if (click == MenuClick.LEFT) port.openTitle()
            clearOnDrop(click, action.sendTitle != null) { it.sendTitle = null }
        }

        pane.on(
            SOUND_SLOT,
            Ui.field(
                "NOTE_BLOCK",
                "Sound",
                action.playSound?.let { "<bd>Sound <val>${NpcFormat.plain(it)}" },
                "<key>Left <hnt>Pick a sound",
            ),
        ) { click ->
            if (click == MenuClick.LEFT) port.pickSound()
            clearOnDrop(click, action.playSound != null) { it.playSound = null }
        }

        pane.on(
            COMMAND_SLOT,
            Ui.field(
                "COMMAND_BLOCK",
                "Run Command",
                action.executeCommand?.let { "<bd>/<val>${NpcFormat.plain(it)}" },
                "<key>Left <hnt>Type it in chat, without /",
            ),
        ) { click ->
            if (click == MenuClick.LEFT) chat(
                "Run Command",
                "Type the command the player runs, without the leading /.",
                action.executeCommand,
                format = PromptFormat.PLAIN,
                clear = { it.executeCommand = null },
                validate = { if (it.isBlank()) PromptResult.Rejected(Component.text("Type a command.")) else null },
            ) { a, text -> a.executeCommand = text.removePrefix("/") }
            clearOnDrop(click, action.executeCommand != null) { it.executeCommand = null }
        }

        pane.on(
            ACTION_BAR_SLOT,
            Ui.field(
                "NAME_TAG",
                "Action Bar",
                action.actionBar?.let { "<val>${Ui.quote(it)}" },
                "<key>Left <hnt>Type it in chat",
            ),
        ) { click ->
            if (click == MenuClick.LEFT) {
                chat(
                    "Action Bar",
                    "Type the action bar text in chat.",
                    action.actionBar,
                    clear = { it.actionBar = null },
                ) { a, text -> a.actionBar = text }
            }
            clearOnDrop(click, action.actionBar != null) { it.actionBar = null }
        }

        when (port.scope) {
            Scope.NPC -> {
                val animation = action.npcAnimation
                val animationLines = Ui.cycleLines(ANIMATIONS, animation, ::animationLabel)
                pane.on(
                    SCOPED_FIRST_SLOT,
                    Ui.item(
                        "FEATHER",
                        "<ttl>NPC Animation",
                        animationLines + listOf(
                            "",
                            "<hnt>Only the clicking player sees it.",
                            "",
                            "<key>Left <hnt>Next",
                        ),
                        glowing = animation != null,
                    ),
                ) { click ->
                    if (click == MenuClick.LEFT) {
                        val next = ANIMATIONS.after(animation)
                        port.edit { it.npcAnimation = next }
                    }
                    clearOnDrop(click, animation != null) { it.npcAnimation = null }
                }

                pane.on(
                    SCOPED_SECOND_SLOT,
                    Ui.field(
                        "PAPER",
                        "Speech Bubble",
                        action.speech?.let { "<val>${Ui.quote(it)}" },
                        "<key>Left <hnt>Type what the NPC says",
                    ),
                ) { click ->
                    if (click == MenuClick.LEFT) chat(
                        "Speech Bubble",
                        "Type what the NPC says above its head.",
                        action.speech,
                        clear = { it.speech = null },
                    ) { a, text -> a.speech = text }
                    clearOnDrop(click, action.speech != null) { it.speech = null }
                }
            }

            Scope.MENU -> {
                pane.toggle(
                    SCOPED_FIRST_SLOT,
                    "Close Menu",
                    action.closeMenu,
                    extraLore = listOf("<hnt>Closes the menu after the click."),
                    material = "OAK_DOOR",
                ) { port.edit { it.closeMenu = !action.closeMenu } }

                pane.toggle(
                    SCOPED_SECOND_SLOT,
                    "Previous Menu",
                    action.previousMenu,
                    extraLore = listOf("<hnt>Goes back to the menu", "<hnt>this one was opened from."),
                    material = "ARROW",
                ) { port.edit { it.previousMenu = !action.previousMenu } }
            }
        }

        pane[COOLDOWN_SLOT] = COOLDOWN.element(port.textPrompts, port.player, action.cooldown / 1000.0) { seconds ->
            port.edit { it.cooldown = (seconds * 1000).toLong() }
        }

        val permission = action.permission?.takeIf { it.isNotBlank() }
        val denyMessage = action.denyMessage
        pane.on(
            PERMISSION_SLOT,
            Ui.item(
                "IRON_DOOR",
                "<ttl>Permission",
                listOfNotNull(
                    permission?.let { "<bd>Needs <val>$it" } ?: "<off>Everyone may use it",
                    permission?.let {
                        when {
                            denyMessage == null -> "<bd>Others see the default message"
                            denyMessage.isBlank() -> "<bd>Others are ignored silently"
                            else -> "<bd>Others see <val>${Ui.quote(denyMessage)}"
                        }
                    },
                    "",
                    "<key>Left <hnt>Type the permission",
                    "<key>Right <hnt>Type what others see".takeIf { permission != null },
                    "<key>Q <hnt>Everyone again".takeIf { permission != null },
                ),
                glowing = permission != null,
            ),
        ) { click ->
            when (click) {
                MenuClick.LEFT -> chat(
                    "Permission",
                    "Type the permission a player needs.",
                    permission,
                    format = PromptFormat.PLAIN,
                    placeholders = emptyList(),
                    validate = {
                        if (it.isBlank() || it.any(Char::isWhitespace)) {
                            PromptResult.Rejected(Component.text("No spaces in a permission."))
                        } else {
                            null
                        }
                    },
                ) { a, text -> a.permission = text }
                MenuClick.RIGHT -> if (permission != null) chat(
                    "Deny Message",
                    "Type what players without it see, or {} to show nothing.",
                    denyMessage?.ifBlank { SILENT },
                    args = listOf(SILENT),
                ) { a, text -> a.denyMessage = if (text.equals(SILENT, true)) "" else text }
                MenuClick.DROP -> if (permission != null) port.edit {
                    it.permission = null
                    it.denyMessage = null
                }
                else -> Unit
            }
        }
    }

    private fun animationLabel(animation: NpcAnimation?): String = when (animation) {
        null -> "None"
        NpcAnimation.SWING_MAIN_HAND -> "Swing arm"
        NpcAnimation.SWING_OFF_HAND -> "Swing off hand"
        NpcAnimation.CROUCH -> "Crouch"
    }
}
