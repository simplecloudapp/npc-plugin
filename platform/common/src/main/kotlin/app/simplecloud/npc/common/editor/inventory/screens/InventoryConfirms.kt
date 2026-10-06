package app.simplecloud.npc.common.editor.inventory.screens

import app.simplecloud.npc.common.editor.core.ConfirmGate
import app.simplecloud.npc.common.editor.core.ConfirmSpec
import app.simplecloud.npc.common.editor.inventory.InventoryConfirmPurpose
import app.simplecloud.npc.common.editor.inventory.InventoryEditorContext
import app.simplecloud.npc.common.editor.inventory.InventoryEditorScreen
import app.simplecloud.npc.common.editor.inventory.LiveGroups
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.text.sendSuccess
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer

object InventoryConfirms {

    fun build(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        purpose: InventoryConfirmPurpose,
    ): EditorMenu {
        val screen = InventoryEditorScreen.Confirm(config.id, purpose)
        val spec = when (purpose) {
            InventoryConfirmPurpose.DeleteInventory -> {
                val users = InventoryHubMenuBuilder.usedBy(context, config.id).size +
                    InventoryHubMenuBuilder.menusOpening(context, config.id).size
                val usersLine = if (users == 0) {
                    "<hnt>Nothing opens it."
                } else {
                    "<warn>$users NPC(s) or menus open it and will open nothing."
                }
                ConfirmSpec<InventoryEditorScreen>(
                    title = Ui.title("Delete Menu?", config.id),
                    screen = screen,
                    verb = "Delete",
                    noun = "deletion",
                    head = Ui.item(
                        "CHEST",
                        "<err><b>Delete ${config.id}?",
                        listOf(
                            "<bd><val>${config.items.size} <bd>items · " +
                                "<val>${config.liveGroups().size} <bd>live groups",
                            usersLine,
                            "",
                            "<err>The file goes. There is no undo.",
                        ),
                        glowing = true,
                    ),
                    fire = {
                        context.inventoryManager.delete(config.id)
                        player.sendSuccess("Deleted menu {}.", config.id)
                        context.close(player)
                    },
                )
            }

            is InventoryConfirmPurpose.DeleteLiveGroup -> {
                val slots = LiveGroups.members(config, purpose.group).size
                ConfirmSpec<InventoryEditorScreen>(
                    title = Ui.title("Delete Live Group?", purpose.group),
                    screen = screen,
                    verb = "Delete",
                    noun = "deletion",
                    head = Ui.item(
                        "PAPER",
                        "<err><b>Delete live group ${purpose.group}?",
                        listOf(
                            "<bd><val>$slots <bd>slots, page arrows included.",
                            "",
                            "<hnt>Undo on the canvas brings it back.",
                        ),
                        glowing = true,
                    ),
                    fire = {
                        context.change(player, config.id) { LiveGroups.delete(it, purpose.group) }
                        val stack = context.session(player).stack.toList()
                        val anchor = stack.indexOfLast {
                            it is InventoryEditorScreen.Canvas || it is InventoryEditorScreen.Hub
                        }
                        context.back(player, if (anchor == -1) 1 else stack.lastIndex - anchor)
                    },
                )
            }
        }

        return ConfirmGate.build(context, player, spec)
    }
}
