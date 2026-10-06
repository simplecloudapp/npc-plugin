package app.simplecloud.npc.common.editor.npc.tabs

import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.common.editor.core.armed
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.NpcTab
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.manager.NpcFailure
import app.simplecloud.npc.common.manager.NpcOperationResult
import app.simplecloud.npc.core.config.ConfigIds
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer
import net.kyori.adventure.text.Component

object NpcTabBuilder {
    private const val SUMMON_SLOT = 20
    private const val DUPLICATE_SLOT = 22
    private const val DELETE_SLOT = 24
    private const val TARGETS_SLOT = 29
    private const val LIVE_SLOT = 31
    private const val NEARBY_SLOT = 33

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): EditorMenu {
        val screen = NpcEditorScreen.Tab(config.id, NpcTab.NPC)
        val live = context.liveData
        val pane = TabFrame.pane(context, player, config, NpcTab.NPC)

        pane.left(
            SUMMON_SLOT,
            Ui.item(
                "ENDER_PEARL",
                "<ttl>Summon To Me",
                listOf("<bd>Now at <val>${NpcFormat.position(config.entity.location)}", "<key>Left <hnt>Move it here"),
            ),
        ) {
            context.npcManager.teleport(config.id, player.location())
            context.render(player, screen)
        }

        pane.left(
            DUPLICATE_SLOT,
            Ui.item(
                "SPAWNER",
                "<ttl>Duplicate",
                listOf("<bd>A copy appears where you stand.", "<key>Left <hnt>Name the copy"),
            ),
        ) { promptDuplicate(context, player, config, screen) }

        pane.armed(
            DELETE_SLOT,
            context,
            player,
            screen,
            "delete-npc",
            idle = Ui.item("BARRIER", "<err>Delete NPC", listOf("<err>There is no undo.", "<key>Left <hnt>Delete")),
            verb = "Delete ${NpcFormat.displayName(config)}",
        ) {
            context.npcManager.delete(config.id)
            context.close(player)
        }

        val targets = config.targetServers
        pane[TARGETS_SLOT] = Ui.item(
            "COMPASS",
            "<ttl>Targets",
            listOfNotNull(
                targets.firstOrNull()?.let { "<val>$it" } ?: "<off>None",
                (targets.size - 1).takeIf { it > 0 }?.let { "<hnt>+$it more" },
            ),
        )

        val joinState = live.hologramState(config).joinState
        pane[LIVE_SLOT] = Ui.item(
            "REDSTONE_TORCH",
            "<ttl>Join State",
            listOf(joinState?.let { "<val>${NpcFormat.joinState(it)}" } ?: "<off>unknown", "<hnt>of the first target"),
        )

        pane[NEARBY_SLOT] = Ui.item(
            "PLAYER_HEAD",
            "<ttl>Nearby",
            listOf("<val>${live.playersInRange(config)} <hnt>players in range"),
        )

        return TabFrame.menu(pane, config)
    }

    private fun promptDuplicate(
        context: NpcEditorContext,
        player: NpcPlayer,
        config: NpcConfig,
        screen: NpcEditorScreen,
    ) {
        context.chatPrompt(
            player,
            Prompt(
                title = "Duplicate NPC",
                instruction = "Type the id of the copy in chat.",
                onCancel = { context.render(player, screen) },
                onSubmit = { input ->
                    when (val result = context.npcManager.duplicate(config.id, input.trim(), player.location())) {
                        is NpcOperationResult.Success -> {
                            context.replace(player, NpcEditorScreen.Tab(result.config.id, NpcTab.NPC))
                            PromptResult.Accepted
                        }

                        is NpcOperationResult.Failure -> PromptResult.Rejected(
                            Component.text(
                                when (result.failure) {
                                    NpcFailure.ALREADY_EXISTS -> "That id is taken."
                                    NpcFailure.INVALID_ID -> ConfigIds.DESCRIPTION
                                    else -> "The copy could not be created."
                                },
                            ),
                        )
                    }
                },
            ),
        )
    }
}
