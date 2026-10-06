package app.simplecloud.npc.common.editor.inventory.screens

import app.simplecloud.npc.common.editor.core.TitlePane
import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.inventory.InventoryEditorContext
import app.simplecloud.npc.common.editor.inventory.InventoryEditorScreen
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.ui.Clicks
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.inventory.ItemClickActions
import app.simplecloud.npc.core.config.NpcConfig.TitleConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer

object ItemTitleMenuBuilder {

    fun build(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.ItemTitle,
    ): EditorMenu {
        val first = config.items.firstOrNull { it.slot == screen.slots.first() }
        val pane = Pane(TitlePane.SIZE)

        fun change(transform: (TitleConfiguration?) -> TitleConfiguration?) {
            context.change(player, config.id) { fresh ->
                fresh.items.filter { it.slot in screen.slots }.forEach { item ->
                    val action = ItemClickActions.orCreate(item, screen.interaction)
                    action.sendTitle = transform(action.sendTitle)
                    item.actions.removeIf { it.isEmpty() }
                }
                fresh
            }
        }

        TitlePane.fill(
            pane,
            TitlePane.Port(
                player = player,
                title = first?.let { ItemClickActions.of(it, screen.interaction) }?.sendTitle,
                textPrompts = context.textPrompts,
                edit = { mutate ->
                    change { TitlePane.normalized(mutate(it ?: TitlePane.defaults())) }
                    context.render(player, screen)
                },
                removeAndBack = {
                    change { null }
                    context.back(player)
                },
            ),
        )

        pane.back(context, player)
        pane.fillNavRow()

        return pane.menu(Ui.title("Title", Clicks.label(screen.interaction)))
    }
}
