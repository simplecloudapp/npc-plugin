package app.simplecloud.npc.common.editor.npc.picker

import app.simplecloud.npc.common.editor.core.PickerOption
import app.simplecloud.npc.common.editor.core.PickerResult
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.npc.PickerPurpose
import app.simplecloud.npc.common.editor.npc.Refresh
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.core.cloud.JoinStates
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer

class AddJoinStateSource(override val purpose: PickerPurpose.AddJoinState) : PickerSource {
    private val forHologram = purpose.forHologram

    override val title = if (forHologram) "Add Layout" else "Add Join State"

    private fun present(config: NpcConfig): List<String> =
        if (forHologram) NpcFormat.hologramJoinStates(config) else NpcFormat.actionJoinStates(config)

    override fun options(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig): List<PickerOption> {
        val present = present(config)
        val atCap = !forHologram && present.size >= NpcConfig.MAX_JOIN_STATES
        val liveTargets = config.targetServers
            .mapNotNull { target -> context.liveData.joinState(target)?.let { it.lowercase() to target } }
            .groupBy({ it.first }, { it.second })

        return JoinStates.offered(config, liveTargets.keys).map { state ->
            val label = NpcFormat.joinState(state)
            val usedLine = "<bd>Used by " + listOfNotNull(
                "<val>hologram".takeIf { config.hologram.findLayout(state) != null },
                "<val>actions".takeIf { state in NpcFormat.actionJoinStates(config) },
            ).ifEmpty { listOf("<off>nothing yet") }.joinToString("<bd>, ")
            val liveLine = liveTargets[state]?.let { "<on>Live now <hnt>on ${it.joinToString(", ")}" }
            val lore = listOfNotNull(usedLine, liveLine)
            when {
                state in present -> PickerOption(
                    label,
                    Ui.unavailable(label, lore, "Already present here."),
                    available = false,
                )

                atCap -> PickerOption(label, Ui.unavailable(label, lore, "No free slot left here."), available = false)
                else -> PickerOption(state, Ui.item("PAPER", "<ttl>$label", lore + listOf("", "<key>Left <hnt>Add")))
            }
        }
    }

    override fun resolveTyped(context: NpcEditorContext, config: NpcConfig, typed: String): PickerResult? {
        val state = typed.removePrefix("@").lowercase()
        val present = present(config)

        return when {
            !JOIN_STATE_PATTERN.matches(state) ->
                PickerResult.Rejected("Invalid", typed, "Letters, digits, _ and - only, up to 24 characters.")

            state in present && forHologram ->
                PickerResult.Rejected("Already present", typed, "That join state already has a layout.")

            state in present -> PickerResult.Rejected("Already present", typed, "That join state already has a column.")

            !forHologram && present.size >= NpcConfig.MAX_JOIN_STATES ->
                PickerResult.Rejected(
                    "Cap reached",
                    typed,
                    "All ${NpcConfig.MAX_JOIN_STATES} join state columns are in use.",
                )

            else -> null
        }
    }

    override fun apply(context: NpcEditorContext, player: NpcPlayer, config: NpcConfig, value: String) {
        val state = value.removePrefix("@").lowercase()
        if (forHologram) {
            context.commitAndBack(player, config.id, refresh = Refresh.HOLOGRAM) { fresh ->
                fresh.apply { hologram.layoutOrCreate(state) }
            }
        } else {
            context.commitAndBack(player, config.id) { fresh ->
                if (fresh.joinStates.any { it.equals(state, true) }) return@commitAndBack null
                if (present(fresh).size >= NpcConfig.MAX_JOIN_STATES) return@commitAndBack null

                fresh.joinStates += state
                fresh
            }
        }
    }

    private companion object {
        private val JOIN_STATE_PATTERN = Regex("^[a-z0-9_-]{1,24}$")
    }
}
