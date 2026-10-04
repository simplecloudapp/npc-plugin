package app.simplecloud.npc.common.editor.inventory.screens

import app.simplecloud.npc.common.editor.core.ActionFieldsPane
import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.inventory.InventoryEditorContext
import app.simplecloud.npc.common.editor.inventory.InventoryEditorScreen
import app.simplecloud.npc.common.editor.inventory.InventoryPickerPurpose
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.ui.Clicks
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.inventory.ItemClickActions
import app.simplecloud.npc.core.config.NpcConfig.ActionConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer

object ItemActionMenuBuilder {

    fun build(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.ItemAction,
    ): EditorMenu {
        val slots = screen.slots
        val first = config.items.firstOrNull { it.slot == slots.first() }
        val action = first?.let { ItemClickActions.of(it, screen.interaction) }
            ?: ActionConfiguration(interactionType = screen.interaction)
        val pane = Pane(ActionFieldsPane.SIZE)

        fun edit(mutate: (ActionConfiguration) -> Unit) {
            context.change(player, config.id) { fresh ->
                fresh.items.filter { it.slot in slots }.forEach { item ->
                    mutate(ItemClickActions.orCreate(item, screen.interaction))
                    item.actions.removeIf { it.isEmpty() }
                }
                fresh
            }
            context.render(player, screen)
        }

        fun pick(purpose: InventoryPickerPurpose) =
            context.navigate(player, InventoryEditorScreen.Picker(config.id, purpose))

        pane[ActionFieldsPane.HEADER_SLOT] = Ui.item(
            "LEVER",
            "<ttl>${Clicks.label(screen.interaction)}",
            listOfNotNull(
                "<bd>What happens when a player clicks this way.",
                "<warn>Applies to ${slots.size} items.".takeIf { slots.size > 1 },
            ),
        )

        ActionFieldsPane.fill(
            pane,
            ActionFieldsPane.Port(
                scope = ActionFieldsPane.Scope.MENU,
                player = player,
                action = action,
                textPrompts = context.textPrompts,
                edit = ::edit,
                toggleJoinTarget = { edit { it.joinTarget = !action.joinTarget } },
                prompt = { context.chatPrompt(player, it) },
                rerender = { context.render(player, screen) },
                pickMenu = { pick(InventoryPickerPurpose.OpenInventory(slots, screen.interaction)) },
                pickServer = { pick(InventoryPickerPurpose.SendToServer(slots, screen.interaction)) },
                pickSound = { pick(InventoryPickerPurpose.Sound(slots, screen.interaction)) },
                openTitle = {
                    context.navigate(player, InventoryEditorScreen.ItemTitle(config.id, slots, screen.interaction))
                },
            ),
        )

        pane.left(
            ActionFieldsPane.WIPE_SLOT,
            Ui.item(
                "TNT",
                "<err>Clear This Click",
                listOf(
                    "<bd>Removes every action of this click type.",
                    "<hnt>Undo on the canvas brings it back.",
                    "",
                    "<key>Left <hnt>Clear",
                ),
            ),
        ) {
            context.change(player, config.id) { fresh ->
                fresh.items
                    .filter { it.slot in slots }
                    .forEach { item -> item.actions.removeIf { it.interactionType == screen.interaction } }
                fresh
            }
            context.back(player)
        }

        pane.back(context, player)
        pane.fillNavRow()

        return pane.menu(Ui.title(Clicks.label(screen.interaction), "item"))
    }
}
