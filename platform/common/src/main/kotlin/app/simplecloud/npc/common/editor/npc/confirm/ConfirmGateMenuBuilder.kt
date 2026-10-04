package app.simplecloud.npc.common.editor.npc.confirm

import app.simplecloud.npc.common.editor.core.ConfirmGate
import app.simplecloud.npc.common.editor.core.ConfirmSpec
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.npc.ConfirmPurpose
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.editor.ui.Clicks
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

object ConfirmGateMenuBuilder {
    private class Wording(val title: String, val verb: String, val noun: String)

    private fun wording(purpose: ConfirmPurpose): Wording = when (purpose) {
        ConfirmPurpose.DeleteNpc -> Wording("Delete NPC?", "Delete", "deletion")
        is ConfirmPurpose.WipeAction -> Wording("Wipe this action?", "Wipe", "wipe")
        is ConfirmPurpose.RemoveJoinState -> Wording(
            "Remove ${NpcFormat.joinState(purpose.joinState)}?",
            "Remove",
            "removal",
        )

        is ConfirmPurpose.RemoveLayout -> Wording(
            "Remove ${NpcFormat.joinState(purpose.joinState)} layout?",
            "Remove",
            "removal",
        )
    }

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, purpose: ConfirmPurpose): EditorMenu {
        val words = wording(purpose)
        return ConfirmGate.build(
            context,
            player,
            ConfirmSpec<NpcEditorScreen>(
                title = Ui.title(words.title, NpcFormat.displayName(config)),
                screen = NpcEditorScreen.Confirm(config.id, purpose),
                verb = words.verb,
                noun = words.noun,
                head = head(config, purpose),
                fire = { fire(context, player, config, purpose) },
            ),
        )
    }

    private fun head(config: NpcConfig, purpose: ConfirmPurpose): NpcItem = when (purpose) {
        ConfirmPurpose.DeleteNpc -> {
            val lineCount = config.hologram.layouts.sumOf { it.lines.size }
            val layoutCount = NpcFormat.hologramJoinStates(config).size
            Ui.npcHead(
                config.entity.skin,
                "<err><b>Delete ${NpcFormat.displayName(config)}?",
                listOf(
                    "<bd><val>${config.targetServers.size} <bd>targets",
                    "<bd><val>$layoutCount <bd>hologram layouts · <val>$lineCount <bd>lines",
                    "<bd><val>${config.actions.count { it.configuredTypes().isNotEmpty() }} <bd>configured actions",
                    "",
                    "<err>All of it goes. There is no undo",
                    "<err>and no backup of this NPC.",
                ),
            )
        }

        is ConfirmPurpose.WipeAction -> {
            val action = config.findAction(purpose.interaction, purpose.joinState)
            val joinTargetLine = listOfNotNull("<on>Counts as join target".takeIf { action?.joinTarget == true })
            val fields = (joinTargetLine + action?.let(NpcFormat::fieldLines).orEmpty())
                .ifEmpty { listOf("<off>Nothing is configured.") }
            Ui.item(
                Clicks.material(purpose.interaction),
                "<err><b>Wipe ${NpcFormat.cellName(purpose.interaction, purpose.joinState)}?",
                fields + listOf(
                    "",
                    "<err>All ${action?.configuredTypes()?.size ?: 0} fields go. There is no undo.",
                ),
                glowing = true,
            )
        }

        is ConfirmPurpose.RemoveJoinState -> {
            val layout = config.hologram.findLayout(purpose.joinState)
            val actionCount = config.actions.count {
                it.joinState.equals(purpose.joinState, true) && it.configuredTypes().isNotEmpty()
            }

            Ui.item(
                "PAPER",
                "<err><b>Remove ${NpcFormat.joinState(purpose.joinState)}?",
                listOf(
                    layout?.let { "<bd>Hologram layout <val>yes <bd>· <val>${it.lines.size} <bd>lines" }
                        ?: "<bd>Hologram layout <off>none",
                    "<bd><val>$actionCount <bd>configured actions",
                    "",
                    "<err>Both go. There is no undo.",
                ),
                glowing = true,
            )
        }

        is ConfirmPurpose.RemoveLayout -> {
            val lines = config.hologram.findLayout(purpose.joinState)?.lines?.size ?: 0
            Ui.item(
                "PAPER",
                "<err><b>Remove ${NpcFormat.joinState(purpose.joinState)} layout?",
                listOf(
                    "<bd><val>$lines <bd>hologram lines",
                    "<hnt>Actions for ${NpcFormat.joinState(purpose.joinState)} are kept.",
                    "",
                    "<err>The lines go. There is no undo.",
                ),
                glowing = true,
            )
        }
    }

    private fun fire(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, purpose: ConfirmPurpose) {
        when (purpose) {
            ConfirmPurpose.DeleteNpc -> {
                context.npcManager.delete(config.id)
                context.close(player)
            }

            is ConfirmPurpose.WipeAction -> {
                val stack = context.session(player).stack
                val steps = if (stack.getOrNull(stack.size - 2) is NpcEditorScreen.ActionEditor) 2 else 1

                context.commitAndBack(player, config.id, steps) { fresh ->
                    fresh.actions.removeIf {
                        it.interactionType == purpose.interaction && it.joinState.equals(purpose.joinState, true)
                    }
                    fresh
                }
            }

            is ConfirmPurpose.RemoveJoinState -> context.commitAndBack(
                player,
                config.id,
                refresh = Refresh.HOLOGRAM,
            ) { fresh ->
                val state = purpose.joinState.lowercase()
                fresh.hologram.layouts.removeIf { it.joinState.equals(state, true) }
                fresh.actions.removeIf { it.joinState.equals(state, true) }
                fresh.joinStates.removeIf { it.equals(state, true) }
                fresh
            }

            is ConfirmPurpose.RemoveLayout -> context.commitAndBack(
                player,
                config.id,
                refresh = Refresh.HOLOGRAM,
            ) { fresh ->
                fresh.hologram.layouts.removeIf { it.joinState.equals(purpose.joinState, true) }
                fresh
            }
        }
    }
}
