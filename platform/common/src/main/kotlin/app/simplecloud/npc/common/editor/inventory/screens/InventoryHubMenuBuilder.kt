package app.simplecloud.npc.common.editor.inventory.screens

import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.common.editor.inventory.InventoryConfirmPurpose
import app.simplecloud.npc.common.editor.inventory.InventoryEditorContext
import app.simplecloud.npc.common.editor.inventory.InventoryEditorScreen
import app.simplecloud.npc.common.editor.inventory.InventoryPickerPurpose
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.text.sendSuccess
import app.simplecloud.npc.core.config.ConfigIds
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer
import net.kyori.adventure.text.Component

object InventoryHubMenuBuilder {
    private const val SIZE = 36

    private const val SNAPSHOT_SLOT = 4
    private const val CANVAS_SLOT = 11
    private const val TITLE_SLOT = 13
    private const val RENAME_SLOT = 15
    private const val USED_BY_SLOT = 20
    private const val DUPLICATE_SLOT = 22
    private const val DELETE_SLOT = 24
    private const val UNDO_SLOT = 27
    private const val CLOSE_SLOT = 31

    private val HEADER_FILLER_SLOTS = (0..3) + (5..8)
    private const val DEFAULT_TITLE = "<#0ea5e9>Menu"
    private const val USED_BY_LISTED = 6

    fun usedBy(context: InventoryEditorContext, inventoryId: String): List<NpcConfig> =
        context.npcRepository.findAll()
            .filter { npc -> npc.actions.any { it.openInventory.equals(inventoryId, true) } }
            .sortedBy { it.id.lowercase() }

    fun menusOpening(context: InventoryEditorContext, inventoryId: String): List<InventoryConfiguration> =
        context.inventoryRepository.findAll()
            .filter { menu -> !menu.id.equals(inventoryId, true) && opensMenu(menu, inventoryId) }

    private fun opensMenu(menu: InventoryConfiguration, inventoryId: String): Boolean =
        actionsOf(menu).any { it.openInventory.equals(inventoryId, true) }

    private fun actionsOf(menu: InventoryConfiguration): List<NpcConfig.ActionConfiguration> =
        menu.items.flatMap { item -> item.actions + item.live?.entries.orEmpty().flatMap { it.actions } }

    fun build(context: InventoryEditorContext, player: NpcPlayer, config: InventoryConfiguration): EditorMenu {
        val screen = InventoryEditorScreen.Hub(config.id)
        val users = usedBy(context, config.id)
        val menus = menusOpening(context, config.id)
        val history = context.session(player).history(config.id)
        val pane = Pane(SIZE)

        pane[SNAPSHOT_SLOT] = Ui.item(
            "CHEST",
            "<ttl>Menu <val>${config.id}",
            listOf(
                "<bd>Title " + config.title,
                "<bd>Rows <val>${config.rows} <hnt>· ${config.size()} slots",
                "<bd><val>${config.items.count { it.liveGroup == null }} <bd>items · " +
                    "<val>${config.liveGroups().size} <bd>live groups",
                "<bd>Opened by <val>${users.size} <bd>NPC${if (users.size == 1) "" else "s"} · " +
                    "<val>${menus.size} <bd>menu${if (menus.size == 1) "" else "s"}",
            ),
        )

        pane.left(
            CANVAS_SLOT,
            Ui.item(
                "CRAFTING_TABLE",
                "<ttl>Edit On The Canvas",
                listOf(
                    "<bd>Menu editor",
                    "",
                    "<key>Left <info>Open",
                ),
            ),
        ) { context.navigate(player, InventoryEditorScreen.Canvas(config.id)) }

        pane.on(
            TITLE_SLOT,
            Ui.item(
                "NAME_TAG",
                "<ttl>Title",
                listOf(
                    "<bd>Now: " + config.title,
                    "<hnt>MiniMessage, e.g. <#0ea5e9><bold>Servers",
                    "",
                    "<key>Left <hnt>Type a new one in chat",
                    "<key>Q <hnt>Reset to the default",
                ),
            ),
        ) { click ->
            when (click) {
                MenuClick.LEFT -> context.promptTitle(player, config.id, InventoryEditorScreen.Hub(config.id))
                MenuClick.DROP -> if (context.change(player, config.id) { it.copy(title = DEFAULT_TITLE) }) {
                    context.render(player, screen)
                }

                else -> Unit
            }
        }

        pane.left(
            RENAME_SLOT,
            Ui.item(
                "ANVIL",
                "<ttl>Rename",
                listOf(
                    "<bd>Id <val>${config.id}",
                    "",
                    "<key>Left <hnt>Type a new id in chat",
                ),
            ),
        ) { promptRename(context, player, config) }

        pane.left(
            USED_BY_SLOT,
            Ui.item(
                "ARMOR_STAND",
                "<ttl>Used By",
                if (users.isEmpty()) {
                    listOf("<off>No NPC opens this menu yet.")
                } else {
                    users.take(USED_BY_LISTED).map { "<bd>· <val>${it.id}" } +
                        listOfNotNull(
                            (users.size - USED_BY_LISTED).takeIf { it > 0 }?.let { "<hnt>and $it more" },
                            "",
                            "<key>Left <info>Jump to one of them",
                        )
                },
            ),
        ) {
            if (users.isNotEmpty()) {
                context.navigate(player, InventoryEditorScreen.Picker(config.id, InventoryPickerPurpose.UsedBy))
            }
        }

        pane.left(
            DUPLICATE_SLOT,
            Ui.item(
                "MAP",
                "<ttl>Duplicate",
                listOf("<bd>A copy with a new id, opened here.", "", "<key>Left <hnt>Duplicate"),
            ),
        ) { duplicate(context, player, config) }

        pane.left(
            DELETE_SLOT,
            Ui.item(
                "TNT",
                "<err>Delete This Menu",
                listOf(
                    if (users.isEmpty()) "<hnt>No NPC opens it." else "<warn>${users.size} NPC(s) still open it.",
                    "",
                    "<key>Left <hnt>Asks first",
                ),
            ),
        ) {
            val confirm = InventoryEditorScreen.Confirm(config.id, InventoryConfirmPurpose.DeleteInventory)
            context.navigate(player, confirm)
        }

        pane.on(
            UNDO_SLOT,
            Ui.item(
                if (history.canUndo || history.canRedo) "CLOCK" else "GRAY_DYE",
                "<ttl>Undo / Redo",
                listOf(
                    "<bd><val>${history.undoCount} <bd>steps back · <val>${history.redoCount} <bd>forward",
                    "",
                    "<key>Left <hnt>Undo  <key>Right <hnt>Redo",
                ),
            ),
        ) { click ->
            val moved = when (click) {
                MenuClick.LEFT -> context.undo(player, config.id)
                MenuClick.RIGHT -> context.redo(player, config.id)
                else -> false
            }
            if (moved) context.render(player, screen)
        }

        pane.left(CLOSE_SLOT, Ui.close()) { context.close(player) }
        pane.fill(HEADER_FILLER_SLOTS)
        pane.fillNavRow()

        return pane.menu(Ui.title("⚡ Menu Editor", config.id))
    }

    private fun promptRename(context: InventoryEditorContext, player: NpcPlayer, config: InventoryConfiguration) {
        context.chatPrompt(
            player,
            Prompt(
                title = "Rename Menu",
                instruction = "Type the new id in chat.",
                current = config.id,
                onCancel = { context.render(player, InventoryEditorScreen.Hub(config.id)) },
                onSubmit = { input ->
                    val newId = input.trim()
                    when {
                        !ConfigIds.isValid(newId) -> PromptResult.Rejected(Component.text(ConfigIds.DESCRIPTION))
                        newId.equals(config.id, true) ->
                            PromptResult.Rejected(Component.text("That is the current id."))

                        context.inventoryRepository.find(newId) != null ->
                            PromptResult.Rejected(Component.text("A menu called $newId already exists."))

                        else -> {
                            rename(context, config.id, newId)
                            player.sendSuccess("Renamed menu {} to {}.", config.id, newId)
                            context.reset(player, listOf(InventoryEditorScreen.Hub(newId)))
                            PromptResult.Accepted
                        }
                    }
                },
            ),
        )
    }

    private fun rename(context: InventoryEditorContext, oldId: String, newId: String) {
        val current = context.inventoryRepository.find(oldId) ?: return
        val others = menusOpening(context, oldId)
        context.inventoryRepository.save(relinked(current.copy(id = newId), oldId, newId))
        context.inventoryRepository.delete(current.id)
        others.forEach { context.inventoryRepository.save(relinked(it, oldId, newId)) }

        usedBy(context, oldId).forEach { npc ->
            npc.actions.filter { it.openInventory.equals(oldId, true) }.forEach { it.openInventory = newId }
            context.npcRepository.save(npc)
        }
    }

    private fun relinked(menu: InventoryConfiguration, oldId: String, newId: String): InventoryConfiguration =
        menu.apply {
            actionsOf(this).filter { it.openInventory.equals(oldId, true) }.forEach { it.openInventory = newId }
        }

    private fun duplicate(context: InventoryEditorContext, player: NpcPlayer, config: InventoryConfiguration) {
        val id = generateSequence(1) { it + 1 }
            .map { if (it == 1) "${config.id}-copy" else "${config.id}-copy-$it" }
            .first { context.inventoryRepository.find(it) == null }
        context.inventoryRepository.save(config.deepCopy().copy(id = id))
        player.sendSuccess("Duplicated menu {} as {}.", config.id, id)
        context.reset(player, listOf(InventoryEditorScreen.Hub(id)))
    }
}
