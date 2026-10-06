package app.simplecloud.npc.common.editor.npc

import app.simplecloud.npc.common.editor.ChatInputPrompts
import app.simplecloud.npc.common.editor.canvas.CanvasSurface
import app.simplecloud.npc.common.editor.core.ScreenStackEditor
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.npc.action.ActionEditorMenuBuilder
import app.simplecloud.npc.common.editor.npc.action.TitleEditorMenuBuilder
import app.simplecloud.npc.common.editor.npc.appearance.EquipmentCanvas
import app.simplecloud.npc.common.editor.npc.appearance.GlowMenuBuilder
import app.simplecloud.npc.common.editor.npc.appearance.SkinMenuBuilder
import app.simplecloud.npc.common.editor.npc.confirm.ConfirmGateMenuBuilder
import app.simplecloud.npc.common.editor.npc.hologram.HologramFramesMenuBuilder
import app.simplecloud.npc.common.editor.npc.picker.PickerMenuBuilder
import app.simplecloud.npc.common.editor.npc.tabs.ActionsTabBuilder
import app.simplecloud.npc.common.editor.npc.tabs.BehaviorTabBuilder
import app.simplecloud.npc.common.editor.npc.tabs.HologramTabBuilder
import app.simplecloud.npc.common.editor.npc.tabs.LooksTabBuilder
import app.simplecloud.npc.common.editor.npc.tabs.NpcTabBuilder
import app.simplecloud.npc.common.editor.npc.tabs.TargetsTabBuilder
import app.simplecloud.npc.common.platform.EditorMenuOpener
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

class NpcEditor(
    opener: EditorMenuOpener,
    chatInputPrompts: ChatInputPrompts,
    private val canvasSurface: CanvasSurface,
    liveData: NpcEditorLiveData? = null,
    pluginContextProvider: () -> NpcPluginContext,
) : ScreenStackEditor<NpcEditorScreen, NpcEditorSession>(opener, chatInputPrompts, NpcEditorSessions()) {

    private val context = NpcEditorContext(
        pluginContextProvider,
        chatInputPrompts,
        sessions,
        ::render,
        ::navigate,
        ::replace,
        ::back,
        ::close,
        opener::runSync,
        opener::runLater,
        liveData ?: CloudNpcEditorLiveData(pluginContextProvider),
    )

    private val equipmentCanvas = EquipmentCanvas(context, canvasSurface, ::handleEscape)

    fun open(player: NpcPlayer, config: NpcConfig) = start(player, NpcEditorScreen.Tab(config.id, NpcTab.NPC))

    override fun leavingScreen(session: NpcEditorSession, popped: Boolean) {
        super.leavingScreen(session, popped)
        session.carriedEquipment = null
    }

    override fun show(player: NpcPlayer, screen: NpcEditorScreen) {
        if (screen is NpcEditorScreen.Equipment) {
            opener.runSync {
                val session = sessions.peek(player.uniqueId) ?: return@runSync
                session.menu = null
                opener.detach(player)
                equipmentCanvas.show(player, screen)
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

    override fun exists(screen: NpcEditorScreen): Boolean = context.npcRepository.find(screen.npcId) != null

    override fun build(player: NpcPlayer, screen: NpcEditorScreen): EditorMenu? {
        val config = context.npcRepository.find(screen.npcId) ?: run {
            context.reportMissing(player, screen.npcId)
            return null
        }

        return build(player, config, screen)
    }

    private fun build(player: NpcPlayer, config: NpcConfig, screen: NpcEditorScreen): EditorMenu = when (screen) {
        is NpcEditorScreen.Tab -> when (screen.tab) {
            NpcTab.NPC -> NpcTabBuilder.build(context, player, config)
            NpcTab.LOOKS -> LooksTabBuilder.build(context, player, config)
            NpcTab.BEHAVIOR -> BehaviorTabBuilder.build(context, player, config)
            NpcTab.HOLOGRAM -> HologramTabBuilder.build(context, player, config)
            NpcTab.ACTIONS -> ActionsTabBuilder.build(context, player, config)
            NpcTab.TARGETS -> TargetsTabBuilder.build(context, player, config)
        }

        is NpcEditorScreen.SkinPicker -> SkinMenuBuilder.build(context, player, config)
        is NpcEditorScreen.Glow -> GlowMenuBuilder.build(context, player, config)
        is NpcEditorScreen.Equipment -> error("The equipment editor is not a menu")
        is NpcEditorScreen.HologramFrames -> HologramFramesMenuBuilder.build(context, player, config, screen)
        is NpcEditorScreen.ActionEditor ->
            ActionEditorMenuBuilder.build(context, player, config, screen.interaction, screen.joinState)

        is NpcEditorScreen.ActionBlocks ->
            ActionEditorMenuBuilder.buildBlocks(context, player, config, screen.interaction, screen.joinState)

        is NpcEditorScreen.TitleEditor ->
            TitleEditorMenuBuilder.build(context, player, config, screen.interaction, screen.joinState)

        is NpcEditorScreen.Picker -> PickerMenuBuilder.build(context, player, config, screen.purpose)
        is NpcEditorScreen.Confirm -> ConfirmGateMenuBuilder.build(context, player, config, screen.purpose)
    }
}
