package app.simplecloud.npc.common.editor.inventory

import app.simplecloud.npc.common.editor.ChatInputPrompts
import app.simplecloud.npc.common.editor.canvas.CanvasSurface
import app.simplecloud.npc.common.editor.core.ScreenStackEditor
import app.simplecloud.npc.common.editor.inventory.canvas.CanvasController
import app.simplecloud.npc.common.editor.inventory.screens.InventoryConfirms
import app.simplecloud.npc.common.editor.inventory.screens.InventoryHubMenuBuilder
import app.simplecloud.npc.common.editor.inventory.screens.InventoryPickers
import app.simplecloud.npc.common.editor.inventory.screens.ItemActionMenuBuilder
import app.simplecloud.npc.common.editor.inventory.screens.ItemTitleMenuBuilder
import app.simplecloud.npc.common.editor.inventory.screens.LiveGroupMenuBuilder
import app.simplecloud.npc.common.editor.inventory.screens.LoreMenuBuilder
import app.simplecloud.npc.common.editor.inventory.screens.StateLooksMenuBuilder
import app.simplecloud.npc.common.editor.inventory.screens.StudioMenuBuilder
import app.simplecloud.npc.common.editor.inventory.screens.TemplatesMenuBuilder
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.platform.EditorMenuOpener
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer

class InventoryEditor(
    opener: EditorMenuOpener,
    chatInputPrompts: ChatInputPrompts,
    private val canvasSurface: CanvasSurface,
    pluginContextProvider: () -> NpcPluginContext,
) : ScreenStackEditor<InventoryEditorScreen, InventoryEditorSession>(
    opener,
    chatInputPrompts,
    InventoryEditorSessions(),
) {

    val context = InventoryEditorContext(
        pluginContextProvider,
        chatInputPrompts,
        sessions,
        ::render,
        ::navigate,
        ::replace,
        ::reset,
        ::back,
        ::close,
        opener::runSync,
        opener::runLater,
    )

    private val canvas = CanvasController(context, canvasSurface, ::handleEscape)

    fun open(player: NpcPlayer, config: InventoryConfiguration) = start(player, InventoryEditorScreen.Hub(config.id))

    fun openCreate(player: NpcPlayer) = start(player, InventoryEditorScreen.Templates)

    override fun exists(screen: InventoryEditorScreen): Boolean =
        screen !is InventoryEditorScreen.Bound || context.inventoryRepository.find(screen.inventoryId) != null

    override fun leavingScreen(session: InventoryEditorSession, popped: Boolean) {
        super.leavingScreen(session, popped)
        if (session.stack.lastOrNull() !is InventoryEditorScreen.Canvas) return
        if (popped) session.leaveCanvas() else session.carried = null
    }

    override fun show(player: NpcPlayer, screen: InventoryEditorScreen) {
        if (screen is InventoryEditorScreen.Canvas) {
            opener.runSync {
                val session = sessions.peek(player.uniqueId) ?: return@runSync
                if (context.inventoryRepository.find(screen.inventoryId) == null) {
                    context.reportMissing(player, screen.inventoryId)
                    close(player)
                    return@runSync
                }
                session.menu = null
                opener.detach(player)
                canvas.show(player, screen)
            }
            return
        }

        canvasSurface.detach(player)
        super.show(player, screen)
    }

    override fun close(player: NpcPlayer) {
        canvasSurface.detach(player)
        super.close(player)
    }

    fun refresh(players: List<NpcPlayer>) {
        refreshOpenScreens(players)
        opener.runSync { players.forEach(canvas::refresh) }
    }

    override fun build(player: NpcPlayer, screen: InventoryEditorScreen): EditorMenu? {
        if (screen == InventoryEditorScreen.Templates) return TemplatesMenuBuilder.build(context, player)
        val bound = screen as InventoryEditorScreen.Bound
        val config = context.inventoryRepository.find(bound.inventoryId) ?: run {
            context.reportMissing(player, bound.inventoryId)
            return null
        }

        return when (bound) {
            is InventoryEditorScreen.Hub -> InventoryHubMenuBuilder.build(context, player, config)
            is InventoryEditorScreen.Studio -> StudioMenuBuilder.build(context, player, config, bound)
            is InventoryEditorScreen.Lore -> LoreMenuBuilder.build(context, player, config, bound)
            is InventoryEditorScreen.ItemAction -> ItemActionMenuBuilder.build(context, player, config, bound)
            is InventoryEditorScreen.ItemActionBlocks ->
                ItemActionMenuBuilder.buildBlocks(context, player, config, bound)

            is InventoryEditorScreen.ItemTitle -> ItemTitleMenuBuilder.build(context, player, config, bound)
            is InventoryEditorScreen.LiveGroup -> LiveGroupMenuBuilder.build(context, player, config, bound.group)
            is InventoryEditorScreen.StateLooks -> StateLooksMenuBuilder.build(context, player, config, bound.group)
            is InventoryEditorScreen.Picker -> InventoryPickers.build(context, player, config, bound.purpose)
            is InventoryEditorScreen.Confirm -> InventoryConfirms.build(context, player, config, bound.purpose)
            is InventoryEditorScreen.Canvas -> error("The canvas is not a menu")
        }
    }
}
