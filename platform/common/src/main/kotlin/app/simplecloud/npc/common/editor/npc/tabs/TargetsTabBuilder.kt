package app.simplecloud.npc.common.editor.npc.tabs

import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcTab
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.editor.ui.after
import app.simplecloud.npc.core.config.JoinStrategy
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

object TargetsTabBuilder {
    private val TARGET_SLOTS = (19..25) + (28..34)
    private const val STRATEGY_SLOT = 40

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): EditorMenu {
        val live = context.liveData
        val screen = NpcEditorScreen.Tab(config.id, NpcTab.TARGETS)
        val pane = TabFrame.pane(context, player, config, NpcTab.TARGETS)
        val targets = config.targetServers
        val removable = targets.size > 1

        targets.zip(TARGET_SLOTS).forEach { (target, slot) ->
            val summary = live.summaryOf(target)
            val state = when {
                !summary.known -> "<hnt>Waiting for the cloud…"
                summary.liveServers == 0 -> "<warn>Offline"
                else -> "<val>${summary.liveServers} <hnt>servers · <val>${summary.players} <hnt>players"
            }
            val lore = listOf(
                "<hnt>${if (summary.isGroup) "Group" else "Server"}",
                state,
                if (removable) "<key>Right <err>Remove" else "<hnt>The last target stays.",
            )
            pane.on(slot, Ui.item(if (summary.isGroup) "BOOKSHELF" else "BOOK", "<ttl>$target", lore)) { click ->
                if (click != MenuClick.RIGHT || !removable) return@on
                context.commit(player, config.id, screen, Refresh.HOLOGRAM) { fresh ->
                    if (fresh.targetServers.size <= 1) return@commit null
                    fresh.targetServers.remove(target)
                    fresh
                }
            }
        }

        if (targets.size < NpcConfig.MAX_TARGETS) {
            val add = Ui.addHead("Add Target", listOf("<key>Left <hnt>Pick a group or server"))
            pane.left(TARGET_SLOTS[targets.size], add) {
                context.navigate(player, NpcEditorScreen.Picker(config.id, PickerPurpose.AddTarget))
            }
        }

        pane.left(
            STRATEGY_SLOT,
            Ui.item(
                "HOPPER",
                "<ttl>Join Strategy",
                listOf("<val>${strategyLabel(config.joinStrategy)}", "<key>Left <hnt>Next"),
            ),
        ) {
            context.commit(player, config.id, screen) { fresh ->
                fresh.copy(joinStrategy = JoinStrategy.entries.after(fresh.joinStrategy))
            }
        }

        return TabFrame.menu(pane, config)
    }

    private fun strategyLabel(strategy: JoinStrategy): String = when (strategy) {
        JoinStrategy.LEAST_PLAYERS -> "Emptiest server first"
        JoinStrategy.MOST_PLAYERS -> "Fill servers up"
        JoinStrategy.RANDOM -> "Random server"
    }
}
