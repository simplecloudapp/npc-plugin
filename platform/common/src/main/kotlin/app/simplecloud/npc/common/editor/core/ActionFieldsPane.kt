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
import app.simplecloud.npc.core.config.NpcAnimation
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.config.NpcConfig.ActionConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer
import net.kyori.adventure.text.Component

object ActionFieldsPane {
    const val SIZE = 45
    const val HEADER_SLOT = 4
    const val DELETE_SLOT = 44

    private const val COOLDOWN_SLOT = 40
    private val BLOCK_SLOTS = (10..16) + (19..25) + (28..34)

    const val ADD_SIZE = 45
    private const val ADD_COLUMNS = 8

    private const val SILENT = "silent"
    private val ANIMATIONS = NpcAnimation.entries

    private val COOLDOWN = Stepper(
        label = "Cooldown",
        material = "CLOCK",
        range = 0.0..60.0,
        step = 0.5,
        default = NpcConfig.DEFAULT_COOLDOWN_MILLIS / 1000.0,
        decimals = 1,
        unit = "s",
        hints = listOf("<hnt>Clicks inside it are ignored."),
    )

    enum class Scope { NPC, MENU }

    class Port(
        val scope: Scope,
        val player: NpcPlayer,
        val action: ActionConfiguration,
        val edit: ((ActionConfiguration) -> Unit) -> Unit,
        val toggleJoinTarget: () -> Unit,
        val prompt: (Prompt) -> Unit,
        val rerender: () -> Unit,
        val pickMenu: () -> Unit,
        val pickServer: () -> Unit,
        val pickSound: () -> Unit,
        val openTitle: () -> Unit,
        val openAdd: () -> Unit,
        val closeAdd: () -> Unit,
    )

    private enum class Group(val label: String, val material: String) {
        GO_TO("Go To", "LIGHT_BLUE_STAINED_GLASS_PANE"),
        MESSAGE("Message", "YELLOW_STAINED_GLASS_PANE"),
        EFFECT("Effect", "LIME_STAINED_GLASS_PANE"),
        ADVANCED("Advanced", "ORANGE_STAINED_GLASS_PANE"),
    }

    private class Block(
        val group: Group,
        val material: String,
        val label: String,
        val describe: String,
        val scope: Scope? = null,
        val available: (ActionConfiguration) -> Boolean = { true },
        val summary: (ActionConfiguration) -> String?,
        val editHint: String? = "Edit",
        val edit: ((Port) -> Unit)?,
        val add: (Port) -> Unit = { port -> edit?.invoke(port) },
        val clear: (ActionConfiguration) -> Unit,
    )

    private val BLOCKS: List<Block> = listOf(
        Block(
            Group.GO_TO, "NETHER_STAR", "Join Target", "Sends the player to the best target server.",
            summary = { a -> "<on>On".takeIf { a.joinTarget } },
            editHint = null,
            edit = null,
            add = { port -> port.toggleJoinTarget() },
            clear = { it.joinTarget = false },
        ),
        Block(
            Group.GO_TO, "CHEST", "Open Menu", "Opens one of your menus.",
            summary = { a -> a.openInventory?.let { "<val>${NpcFormat.plain(it)}" } },
            edit = { it.pickMenu() },
            clear = { it.openInventory = null },
        ),
        Block(
            Group.GO_TO, "COMPASS", "Send To Server", "Sends the player to one server.",
            summary = { a -> a.sendToServer?.let { "<val>${NpcFormat.plain(it)}" } },
            edit = { it.pickServer() },
            clear = { it.sendToServer = null },
        ),
        Block(
            Group.GO_TO, "RECOVERY_COMPASS", "Teleport", "Teleports the player to where you stand.",
            summary = { a -> a.teleport?.let { "<val>${NpcFormat.position(it)}" } },
            editHint = "Move here",
            edit = { port ->
                val at = port.player.location()
                port.edit { it.teleport = at }
            },
            clear = { it.teleport = null },
        ),
        Block(
            Group.GO_TO, "ENDER_EYE", "Transfer", "Moves the player to another host.",
            summary = { a -> a.transferToServer?.let { "<val>${NpcFormat.plain(it)}" } },
            edit = { port ->
                chat(
                    port,
                    "Transfer To Host",
                    "Type the host to transfer to in chat, as host:port.",
                    port.action.transferToServer,
                    format = PromptFormat.PLAIN,
                    placeholders = emptyList(),
                    clear = { it.transferToServer = null },
                    validate = {
                        if (InputChecks.isValidHostPort(it)) null
                        else PromptResult.Rejected(Component.text("Expected host:port."))
                    },
                ) { a, text -> a.transferToServer = text }
            },
            clear = { it.transferToServer = null },
        ),
        Block(
            Group.GO_TO, "OAK_DOOR", "Close Menu", "Closes the menu after the click.", Scope.MENU,
            summary = { a -> "<on>On".takeIf { a.closeMenu } },
            editHint = null,
            edit = null,
            add = { port -> port.edit { it.closeMenu = true } },
            clear = { it.closeMenu = false },
        ),
        Block(
            Group.GO_TO, "ARROW", "Previous Menu", "Goes back to the menu before.", Scope.MENU,
            summary = { a -> "<on>On".takeIf { a.previousMenu } },
            editHint = null,
            edit = null,
            add = { port -> port.edit { it.previousMenu = true } },
            clear = { it.previousMenu = false },
        ),
        Block(
            Group.MESSAGE, "WRITABLE_BOOK", "Chat Message", "Sends the player a chat message.",
            summary = { a ->
                NpcFormat.messageLines(a.sendMessage).takeIf { it.isNotEmpty() }?.let { lines ->
                    "<val>${Ui.quote(lines.first())}" + if (lines.size > 1) " <hnt>+${lines.size - 1}" else ""
                }
            },
            edit = { port ->
                chat(
                    port,
                    "Chat Message",
                    "Type the chat message, {} starts a new line.",
                    port.action.sendMessage?.let { NpcFormat.messageLines(it).joinToString("<newline>") },
                    args = listOf("<newline>"),
                    clear = { it.sendMessage = null },
                ) { a, text -> a.sendMessage = text }
            },
            clear = { it.sendMessage = null },
        ),
        Block(
            Group.MESSAGE, "PAINTING", "Title", "Shows a big title on screen.",
            summary = { a -> a.sendTitle?.let { "<val>${Ui.quote(it.title.ifBlank { it.subtitle })}" } },
            edit = { it.openTitle() },
            clear = { it.sendTitle = null },
        ),
        Block(
            Group.MESSAGE, "NAME_TAG", "Action Bar", "Shows text above the hotbar.",
            summary = { a -> a.actionBar?.let { "<val>${Ui.quote(it)}" } },
            edit = { port ->
                chat(
                    port,
                    "Action Bar",
                    "Type the action bar text in chat.",
                    port.action.actionBar,
                    clear = { it.actionBar = null },
                ) { a, text -> a.actionBar = text }
            },
            clear = { it.actionBar = null },
        ),
        Block(
            Group.MESSAGE, "PAPER", "Speech Bubble", "The NPC says something above its head.", Scope.NPC,
            summary = { a -> a.speech?.let { "<val>${Ui.quote(it)}" } },
            edit = { port ->
                chat(
                    port,
                    "Speech Bubble",
                    "Type what the NPC says above its head.",
                    port.action.speech,
                    clear = { it.speech = null },
                ) { a, text -> a.speech = text }
            },
            clear = { it.speech = null },
        ),
        Block(
            Group.EFFECT, "NOTE_BLOCK", "Sound", "Plays a sound to the player.",
            summary = { a -> a.playSound?.let { "<val>${NpcFormat.plain(it)}" } },
            edit = { it.pickSound() },
            clear = { it.playSound = null },
        ),
        Block(
            Group.EFFECT, "FEATHER", "Animation", "The NPC moves; only this player sees it.", Scope.NPC,
            summary = { a -> a.npcAnimation?.let { "<val>${animationLabel(it)}" } },
            editHint = "Next",
            edit = { port ->
                val next = port.action.npcAnimation?.let(ANIMATIONS::after) ?: ANIMATIONS.first()
                port.edit { it.npcAnimation = next }
            },
            clear = { it.npcAnimation = null },
        ),
        Block(
            Group.EFFECT, "COMMAND_BLOCK", "Command", "The player runs a command.",
            summary = { a -> a.executeCommand?.let { "<val>/${NpcFormat.plain(it)}" } },
            edit = { port ->
                chat(
                    port,
                    "Run Command",
                    "Type the command the player runs, without the leading /.",
                    port.action.executeCommand,
                    format = PromptFormat.PLAIN,
                    clear = { it.executeCommand = null },
                    validate = { if (it.isBlank()) PromptResult.Rejected(Component.text("Type a command.")) else null },
                ) { a, text -> a.executeCommand = text.removePrefix("/") }
            },
            clear = { it.executeCommand = null },
        ),
        Block(
            Group.ADVANCED, "IRON_DOOR", "Permission", "Only players with it may use the click.",
            summary = { a -> a.permission?.takeIf { it.isNotBlank() }?.let { "<val>$it" } },
            edit = { port ->
                chat(
                    port,
                    "Permission",
                    "Type the permission a player needs.",
                    port.action.permission,
                    format = PromptFormat.PLAIN,
                    placeholders = emptyList(),
                    clear = {
                        it.permission = null
                        it.denyMessage = null
                    },
                    validate = {
                        if (it.isBlank() || it.any(Char::isWhitespace)) {
                            PromptResult.Rejected(Component.text("No spaces in a permission."))
                        } else {
                            null
                        }
                    },
                ) { a, text -> a.permission = text }
            },
            clear = {
                it.permission = null
                it.denyMessage = null
            },
        ),
        Block(
            Group.ADVANCED, "OAK_SIGN", "Deny Message", "What players without the permission see.",
            available = { a -> !a.permission.isNullOrBlank() },
            summary = { a ->
                a.denyMessage?.let { if (it.isBlank()) "<hnt>nothing (silent)" else "<val>${Ui.quote(it)}" }
            },
            edit = { port ->
                chat(
                    port,
                    "Deny Message",
                    "Type what players without it see, or {} to show nothing.",
                    port.action.denyMessage?.ifBlank { SILENT },
                    args = listOf(SILENT),
                    clear = { it.denyMessage = null },
                ) { a, text -> a.denyMessage = if (text.equals(SILENT, true)) "" else text }
            },
            clear = { it.denyMessage = null },
        ),
    )

    private fun blocks(scope: Scope): List<Block> = BLOCKS.filter { it.scope == null || it.scope == scope }

    private fun isSet(block: Block, action: ActionConfiguration): Boolean = block.summary(action) != null

    fun fill(pane: Pane, port: Port) {
        val action = port.action
        val set = blocks(port.scope).filter { isSet(it, action) }

        set.zip(BLOCK_SLOTS).forEach { (block, slot) ->
            val lore = listOfNotNull(
                block.summary(action),
                listOfNotNull(
                    block.editHint?.let { "<key>Left <hnt>$it" },
                    "<key>Right <err>Remove",
                ).joinToString(" <hnt>· "),
            )
            pane.on(slot, Ui.item(block.material, "<ttl>${block.label}", lore, glowing = true)) { click ->
                when (click) {
                    MenuClick.LEFT -> block.edit?.invoke(port)
                    MenuClick.RIGHT -> port.edit(block.clear)
                    else -> Unit
                }
            }
        }

        val free = blocks(port.scope).count { !isSet(it, action) && it.available(action) }
        BLOCK_SLOTS.getOrNull(set.size)?.takeIf { free > 0 }?.let { slot ->
            val hint = if (set.isEmpty()) "<hnt>Nothing happens on this click yet." else null
            pane.left(slot, Ui.addHead("Add", listOfNotNull(hint, "<key>Left <hnt>Pick what happens"))) {
                port.openAdd()
            }
        }

        pane[COOLDOWN_SLOT] = COOLDOWN.element(action.cooldown / 1000.0) { seconds ->
            port.edit { it.cooldown = (seconds * 1000).toLong() }
        }
    }

    fun fillAdd(pane: Pane, port: Port) {
        val action = port.action
        val all = blocks(port.scope)

        Group.entries.forEachIndexed { row, group ->
            val first = row * 9
            pane[first] = Ui.item(group.material, "<ttl><b>${group.label}")

            all.filter { it.group == group }.take(ADD_COLUMNS).forEachIndexed { column, block ->
                val slot = first + 1 + column
                when {
                    isSet(block, action) -> pane[slot] = Ui.disabled(block.label, listOf("<hnt>Already added"))
                    !block.available(action) ->
                        pane[slot] = Ui.disabled(block.label, listOf("<hnt>Set a permission first"))

                    else -> pane.left(
                        slot,
                        Ui.item(
                            block.material,
                            "<ttl>${block.label}",
                            listOf("<bd>${block.describe}", "<key>Left <hnt>Add"),
                        ),
                    ) {
                        port.closeAdd()
                        block.add(port)
                    }
                }
            }
        }
    }

    private fun chat(
        port: Port,
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
                onClear = clear?.takeIf { current != null }?.let { { port.edit(it) } },
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

    private fun animationLabel(animation: NpcAnimation): String = when (animation) {
        NpcAnimation.SWING_MAIN_HAND -> "Swing arm"
        NpcAnimation.SWING_OFF_HAND -> "Swing off hand"
        NpcAnimation.CROUCH -> "Crouch"
    }
}
