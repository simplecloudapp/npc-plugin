package app.simplecloud.npc.common.editor.inventory

import app.simplecloud.npc.common.editor.ChatInputPrompts
import app.simplecloud.npc.common.editor.EditorPorts
import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.common.editor.PromptTokens
import app.simplecloud.npc.common.editor.core.EditorHost
import app.simplecloud.npc.common.editor.core.EditorSessions
import app.simplecloud.npc.common.editor.core.ScreenAction
import app.simplecloud.npc.common.editor.core.TextPrompts
import app.simplecloud.npc.common.editor.npc.CloudNpcEditorLiveData
import app.simplecloud.npc.common.editor.npc.NpcEditorLiveData
import app.simplecloud.npc.common.manager.InventoryManager
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.text.sendError
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer

class InventoryEditorContext(
    private val pluginContextProvider: () -> NpcPluginContext,
    override val chatInputPrompts: ChatInputPrompts,
    override val sessions: EditorSessions<InventoryEditorSession>,
    render: ScreenAction<InventoryEditorScreen>,
    navigate: ScreenAction<InventoryEditorScreen>,
    replace: ScreenAction<InventoryEditorScreen>,
    private val resetTo: (NpcPlayer, List<InventoryEditorScreen>) -> Unit,
    private val backSteps: (NpcPlayer, Int) -> Unit,
    close: (NpcPlayer) -> Unit,
    runSync: (() -> Unit) -> Unit,
    runLater: (Long, () -> Unit) -> Unit,
) : EditorPorts(
    pluginContextProvider,
    TextPrompts.chat(sessions, chatInputPrompts, render, ::contextOf),
    runSync,
    runLater,
), EditorHost<InventoryEditorScreen> {

    private val renderScreen = render
    private val navigateTo = navigate
    private val replaceWith = replace
    private val closeEditor = close

    val inventoryManager: InventoryManager get() = pluginContextProvider().inventoryManager
    val liveData: NpcEditorLiveData by lazy { CloudNpcEditorLiveData(pluginContextProvider) }

    override fun render(player: NpcPlayer, screen: InventoryEditorScreen) = renderScreen(player, screen)
    override fun promptContext(screen: InventoryEditorScreen): String? = contextOf(screen)
    override fun navigate(player: NpcPlayer, screen: InventoryEditorScreen) = navigateTo(player, screen)
    override fun back(player: NpcPlayer, steps: Int) = backSteps(player, steps)
    override fun close(player: NpcPlayer) = closeEditor(player)
    fun replace(player: NpcPlayer, screen: InventoryEditorScreen) = replaceWith(player, screen)
    fun reset(player: NpcPlayer, screens: List<InventoryEditorScreen>) = resetTo(player, screens)

    override fun session(player: NpcPlayer): InventoryEditorSession = sessions.of(player.uniqueId)

    fun materialCatalog(): List<String> = catalogs.materials()
    fun dataSourceTypes(): List<String> = inventoryViews.dataSourceTypes()

    fun change(
        player: NpcPlayer,
        inventoryId: String,
        mutate: (InventoryConfiguration) -> InventoryConfiguration?,
    ): Boolean {
        val fresh = find(player, inventoryId) ?: return false
        val updated = mutate(fresh.deepCopy()) ?: return false
        if (updated == fresh) return false

        session(player).history(inventoryId).record(fresh)
        save(updated)

        return true
    }

    fun undo(player: NpcPlayer, inventoryId: String): Boolean = travel(player, inventoryId, EditHistory::undo)

    fun redo(player: NpcPlayer, inventoryId: String): Boolean = travel(player, inventoryId, EditHistory::redo)

    fun promptTitle(player: NpcPlayer, inventoryId: String, screen: InventoryEditorScreen) {
        chatPrompt(
            player,
            Prompt(
                title = "Menu Title",
                instruction = "Type the menu title in chat.",
                format = PromptFormat.MINI_MESSAGE,
                placeholders = PromptTokens.MENU,
                current = inventoryRepository.find(inventoryId)?.title,
                onCancel = { render(player, screen) },
                onSubmit = { input ->
                    val title = input.trim()
                    PromptResult.rejectInvalidMiniMessage(title)?.let { return@Prompt it }
                    change(player, inventoryId) { it.copy(title = title) }
                    render(player, screen)
                    PromptResult.Accepted
                },
            ),
        )
    }

    fun reportMissing(player: NpcPlayer, inventoryId: String) {
        player.sendError("Inventory {} no longer exists.", inventoryId)
    }

    private fun travel(
        player: NpcPlayer,
        inventoryId: String,
        step: (EditHistory, InventoryConfiguration) -> InventoryConfiguration?,
    ): Boolean {
        val fresh = find(player, inventoryId) ?: return false
        val target = step(session(player).history(inventoryId), fresh) ?: return false
        save(target)

        return true
    }

    private fun find(player: NpcPlayer, inventoryId: String): InventoryConfiguration? =
        inventoryRepository.find(inventoryId) ?: run {
            reportMissing(player, inventoryId)
            close(player)
            null
        }

    private fun save(config: InventoryConfiguration) {
        inventoryRepository.save(config)
        inventoryViews.requestRefresh()
    }
}

private fun contextOf(screen: InventoryEditorScreen): String? =
    (screen as? InventoryEditorScreen.Bound)?.let { "Menu ${it.inventoryId}" }
