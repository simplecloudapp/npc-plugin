package app.simplecloud.npc.common.editor.npc

import app.simplecloud.npc.common.editor.ChatInputPrompts
import app.simplecloud.npc.common.editor.EditorPorts
import app.simplecloud.npc.common.editor.core.EditorHost
import app.simplecloud.npc.common.editor.core.EditorSessions
import app.simplecloud.npc.common.editor.core.ScreenAction
import app.simplecloud.npc.common.editor.core.TextPrompts
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.text.sendError
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

enum class Refresh {
    NONE,
    ENTITY,
    HOLOGRAM,
    ALL,
}

class NpcEditorContext(
    pluginContextProvider: () -> NpcPluginContext,
    override val chatInputPrompts: ChatInputPrompts,
    override val sessions: EditorSessions<NpcEditorSession>,
    render: ScreenAction<NpcEditorScreen>,
    navigate: ScreenAction<NpcEditorScreen>,
    private val replaceTop: ScreenAction<NpcEditorScreen> = { _, _ -> },
    private val backSteps: (NpcPlayer, Int) -> Unit,
    close: (NpcPlayer) -> Unit,
    runSync: (() -> Unit) -> Unit,
    runLater: (Long, () -> Unit) -> Unit,
    val liveData: NpcEditorLiveData,
) : EditorPorts(
    pluginContextProvider,
    TextPrompts.chat(sessions, chatInputPrompts, render, ::contextOf),
    runSync,
    runLater,
), EditorHost<NpcEditorScreen> {
    private val renderScreen = render
    private val navigateTo = navigate
    private val closeEditor = close

    override fun render(player: NpcPlayer, screen: NpcEditorScreen) = renderScreen(player, screen)
    override fun promptContext(screen: NpcEditorScreen): String? = contextOf(screen)
    override fun navigate(player: NpcPlayer, screen: NpcEditorScreen) = navigateTo(player, screen)
    fun replace(player: NpcPlayer, screen: NpcEditorScreen) = replaceTop(player, screen)

    override fun back(player: NpcPlayer, steps: Int) = backSteps(player, steps)
    override fun close(player: NpcPlayer) = closeEditor(player)

    override fun session(player: NpcPlayer): NpcEditorSession = sessions.of(player.uniqueId)

    fun commit(
        player: NpcPlayer,
        npcId: String,
        then: NpcEditorScreen,
        refresh: Refresh = Refresh.NONE,
        mutate: (NpcConfig) -> NpcConfig?,
    ) {
        if (save(player, npcId, refresh, mutate) != Saved.MISSING) render(player, then)
    }

    fun commitAndBack(
        player: NpcPlayer,
        npcId: String,
        steps: Int = 1,
        refresh: Refresh = Refresh.NONE,
        mutate: (NpcConfig) -> NpcConfig?,
    ) {
        if (save(player, npcId, refresh, mutate) != Saved.MISSING) back(player, steps)
    }

    fun change(
        player: NpcPlayer,
        npcId: String,
        refresh: Refresh = Refresh.NONE,
        mutate: (NpcConfig) -> NpcConfig?,
    ): Boolean = save(player, npcId, refresh, mutate) == Saved.CHANGED

    fun editEntity(
        player: NpcPlayer,
        npcId: String,
        then: NpcEditorScreen,
        mutate: (NpcConfig.NpcEntityConfiguration) -> NpcConfig.NpcEntityConfiguration,
    ) = commit(player, npcId, then, Refresh.ENTITY) { fresh -> fresh.copy(entity = mutate(fresh.entity)) }

    fun reportMissing(player: NpcPlayer, npcId: String) {
        player.sendError("NPC {} no longer exists.", npcId)
    }

    private enum class Saved { MISSING, UNCHANGED, CHANGED }

    private fun save(
        player: NpcPlayer,
        npcId: String,
        refresh: Refresh,
        mutate: (NpcConfig) -> NpcConfig?,
    ): Saved {
        val fresh = npcRepository.find(npcId) ?: run {
            reportMissing(player, npcId)
            close(player)
            return Saved.MISSING
        }
        val updated = mutate(fresh) ?: return Saved.UNCHANGED

        npcRepository.save(updated)
        when (refresh) {
            Refresh.ENTITY -> renderer.refresh(updated)
            Refresh.HOLOGRAM -> hologramRenderer.createOrUpdate(updated)
            Refresh.ALL -> {
                renderer.refresh(updated)
                hologramRenderer.createOrUpdate(updated)
            }
            Refresh.NONE -> Unit
        }

        return Saved.CHANGED
    }
}

private fun contextOf(screen: NpcEditorScreen): String = "NPC ${screen.npcId}"
