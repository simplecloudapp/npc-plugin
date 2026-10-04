package app.simplecloud.npc.common.editor.npc.targets

import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.editor.ui.after
import app.simplecloud.npc.core.config.JoinStrategy
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

object TargetsMenuBuilder {
    private const val SIZE = 45

    private const val SUMMARY_SLOT = 4
    private val TARGET_SLOTS = 9..26
    private const val ADD_SLOT = 30
    private const val STRATEGY_SLOT = 32

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): EditorMenu {
        val live = context.liveData
        val screen = NpcEditorScreen.Targets(config.id)
        val pane = Pane(SIZE)

        val summaries = config.targetServers.associateWith { live.summaryOf(it) }
        val known = live.cloudKnown()

        pane[SUMMARY_SLOT] = Ui.item(
            "COMPASS",
            "<ttl>Deployment",
            listOf(
                "<bd><val>${config.targetServers.size} <bd>targets · " +
                    "<val>${if (known) summaries.values.sumOf { it.liveServers }.toString() else "…"} " +
                    "<bd>live servers",
                "<bd><val>${live.playersInRange(config)} <bd>players currently in range",
                "",
                "<hnt>The NPC exists on every server that",
                "<hnt>matches any target below.",
            ),
        )

        val lastStanding = config.targetServers.size == 1
        config.targetServers.zip(TARGET_SLOTS).forEach { (target, slot) ->
            if (lastStanding) {
                pane[slot] = Ui.disabled(target, listOf("<hnt>The last target cannot be removed."))
            } else {
                val summary = summaries.getValue(target)
                val servers = summary.liveServers
                pane.on(
                    slot,
                    Ui.item(
                        "PAPER",
                        "<ttl>$target",
                        listOfNotNull(
                            "<bd>Type <val>${if (summary.isGroup) "group" else "server"}",
                            if (summary.known) {
                                "<bd>Live <val>$servers <bd>servers · <val>${summary.players} <bd>players"
                            } else {
                                "<hnt>Waiting for cloud data…"
                            },
                            "<warn>Offline".takeIf { summary.known && servers == 0 },
                            "",
                            "<key>Shift+Right <err>Remove target",
                        ),
                    ),
                    MenuClick.SHIFT_RIGHT,
                ) {
                    context.commit(player, config.id, screen, Refresh.HOLOGRAM) { fresh ->
                        if (fresh.targetServers.size <= 1) return@commit null
                        fresh.targetServers.remove(target)
                        fresh
                    }
                }
            }
        }

        if (config.targetServers.size < NpcConfig.MAX_TARGETS) {
            pane.left(
                ADD_SLOT,
                Ui.item(
                    "LIME_DYE",
                    "<ttl>Add Target",
                    listOf(
                        "<bd>Using <val>${config.targetServers.size} <bd>of <val>${NpcConfig.MAX_TARGETS}",
                        "",
                        "<key>Left <info>Open picker",
                    ),
                ),
            ) { context.navigate(player, NpcEditorScreen.Picker(config.id, PickerPurpose.AddTarget)) }
        } else {
            pane[ADD_SLOT] = Ui.disabled(
                "Add Target",
                listOf("<hnt>All ${NpcConfig.MAX_TARGETS} target slots are in use."),
            )
        }

        val strategies = JoinStrategy.entries
        val strategyLines = Ui.cycleLines(strategies, config.joinStrategy, ::strategyLabel)
        pane.left(
            STRATEGY_SLOT,
            Ui.item(
                "HOPPER",
                "<ttl>Join Strategy",
                strategyLines + listOf(
                    "",
                    "<hnt>Prefers servers that aren't full.",
                    "",
                    "<key>Left <hnt>Next strategy",
                ),
            ),
        ) {
            context.commit(player, config.id, screen) { fresh ->
                fresh.copy(joinStrategy = strategies.after(fresh.joinStrategy))
            }
        }

        pane.back(context, player)
        pane.fillNavRow()

        return pane.menu(Ui.title("Targets", NpcFormat.displayName(config)))
    }

    private fun strategyLabel(strategy: JoinStrategy): String = when (strategy) {
        JoinStrategy.LEAST_PLAYERS -> "Emptiest server first"
        JoinStrategy.MOST_PLAYERS -> "Fill servers up"
        JoinStrategy.RANDOM -> "Random server"
    }
}
