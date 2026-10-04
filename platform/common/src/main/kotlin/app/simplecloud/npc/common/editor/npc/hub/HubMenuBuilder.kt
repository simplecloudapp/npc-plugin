package app.simplecloud.npc.common.editor.npc.hub

import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.ConfirmPurpose
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.manager.NpcFailure
import app.simplecloud.npc.common.manager.NpcOperationResult
import app.simplecloud.npc.core.config.ConfigIds
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer
import net.kyori.adventure.text.Component
import java.util.Locale

object HubMenuBuilder {
    private const val SIZE = 54

    private const val SNAPSHOT_SLOT = 4
    private const val APPEARANCE_SLOT = 20
    private const val BEHAVIOR_SLOT = 21
    private const val HOLOGRAM_SLOT = 22
    private const val ACTIONS_SLOT = 23
    private const val TARGETS_SLOT = 24
    private const val SUMMON_SLOT = 38
    private const val DUPLICATE_SLOT = 40
    private const val DELETE_SLOT = 42
    private const val CLOSE_SLOT = 49

    private val HEADER_FILLER_SLOTS = (0..3) + (5..8)

    fun build(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): EditorMenu {
        val entity = config.entity
        val name = NpcFormat.displayName(config)
        val live = context.liveData
        val summaries = config.targetServers.associateWith { live.summaryOf(it) }
        val known = live.cloudKnown()
        val liveServers = if (known) summaries.values.sumOf { it.liveServers }.toString() else "…"
        val pane = Pane(SIZE)

        pane[SNAPSHOT_SLOT] = Ui.npcHead(
            entity.skin,
            "<ttl><b>$name",
            listOf(
                "<hnt>npc:${config.id}",
                "",
                "<bd>Provider <val>${entity.provider}",
                "<bd>Position <val>${NpcFormat.position(entity.location, " · ")}",
                "<bd>Targets <val>${config.targetServers.size} · <val>$liveServers <bd>live servers",
                "<bd>Players in range <val>${live.playersInRange(config)}",
            ),
        )

        pane.left(
            APPEARANCE_SLOT,
            Ui.item(
                "ARMOR_STAND",
                "<ttl>Appearance",
                listOf(
                    "",
                    "<bd>Skin <val>${NpcFormat.skinLabel(entity.skin)}",
                    "<bd>Name <val>$name",
                    NpcFormat.glowLine(entity),
                    "",
                    "<key>Left <info>Open",
                ),
            ),
        ) { context.navigate(player, NpcEditorScreen.Appearance(config.id)) }

        val gazeLine =
            if (entity.lookAtPlayer) {
                "<bd>Gaze <on>on <hnt>· <val>${String.format(Locale.ROOT, "%.1f", entity.lookAtPlayerDistance)}m"
            } else "<bd>Gaze <off>off"
        pane.left(
            BEHAVIOR_SLOT,
            Ui.item(
                "PISTON",
                "<ttl>Behavior",
                listOf(
                    "",
                    gazeLine,
                    if (config.pushback.enabled) "<bd>Push <on>on" else "<bd>Push <off>off",
                    "",
                    "<key>Left <info>Open",
                ),
            ),
        ) { context.navigate(player, NpcEditorScreen.Behavior(config.id)) }

        val hologram = config.hologram
        val liveLayout = live.hologramState(config).shown
        pane.left(
            HOLOGRAM_SLOT,
            Ui.item(
                "GLOW_ITEM_FRAME",
                "<ttl>Hologram",
                listOf(
                    "",
                    if (hologram.enabled) "<bd>State <on>on" else "<bd>State <off>off",
                    liveLayout?.let { "<bd>Live <on>${NpcFormat.joinState(it)}" } ?: "<bd>Live <off>nothing",
                    "",
                    "<key>Left <info>Open",
                ),
            ),
        ) { context.navigate(player, NpcEditorScreen.Hologram(config.id)) }

        pane.left(
            ACTIONS_SLOT,
            Ui.item(
                "LEVER",
                "<ttl>Actions",
                listOf(
                    "",
                    "<bd>Configured actions <val>${config.actions.count { it.configuredTypes().isNotEmpty() }}",
                    "",
                    "<key>Left <info>Open",
                ),
            ),
        ) { context.navigate(player, NpcEditorScreen.ActionMatrix(config.id)) }

        val targetLines = config.targetServers.take(3)
            .map { target ->
                val servers = summaries.getValue(target).takeIf { it.known }?.liveServers?.toString() ?: "…"
                "<val>$target <hnt>$servers servers"
            }
            .ifEmpty { listOf("<off>No targets yet.") } +
            listOfNotNull((config.targetServers.size - 3).takeIf { it > 0 }?.let { "<hnt>+$it more" })
        pane.left(
            TARGETS_SLOT,
            Ui.item("COMPASS", "<ttl>Targets", listOf("") + targetLines + listOf("", "<key>Left <info>Open")),
        ) { context.navigate(player, NpcEditorScreen.Targets(config.id)) }

        pane.on(
            SUMMON_SLOT,
            Ui.item("ENDER_PEARL", "<ttl>Summon To Me", listOf("", "<key>Shift+Right <hnt>Summon")),
            MenuClick.SHIFT_RIGHT,
        ) {
            context.npcManager.teleport(config.id, player.location())
            context.render(player, NpcEditorScreen.Hub(config.id))
        }

        pane.left(
            DUPLICATE_SLOT,
            Ui.item(
                "SPAWNER",
                "<ttl>Duplicate",
                listOf("<bd>A copy appears where you stand.", "", "<key>Left <hnt>Type the copy's id"),
            ),
        ) {
            context.chatPrompt(
                player,
                Prompt(
                    title = "Duplicate NPC",
                    instruction = "Type the id of the copy in chat.",
                    onCancel = { context.render(player, NpcEditorScreen.Hub(config.id)) },
                    onSubmit = { input ->
                        when (val result = context.npcManager.duplicate(config.id, input.trim(), player.location())) {
                            is NpcOperationResult.Success -> {
                                context.replace(player, NpcEditorScreen.Hub(result.config.id))
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

        pane.left(
            DELETE_SLOT,
            Ui.item(
                "BARRIER",
                "<err><b>Delete NPC",
                listOf("", "<err>This cannot be undone.", "", "<key>Left <info>Open"),
            ),
        ) { context.navigate(player, NpcEditorScreen.Confirm(config.id, ConfirmPurpose.DeleteNpc)) }

        pane.left(CLOSE_SLOT, Ui.close()) { context.close(player) }
        pane.fill(HEADER_FILLER_SLOTS)
        pane.fillNavRow()

        return pane.menu(Ui.title("NPC Editor", name))
    }
}
