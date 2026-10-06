package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.PromptTokens
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.ui.Stepper
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.core.config.NpcConfig.TitleConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer

object TitlePane {
    const val SIZE = 27

    private val fadeRange = TitleConfiguration.FADE_RANGE.toDoubleRange()
    private val stayRange = TitleConfiguration.STAY_RANGE.toDoubleRange()

    private const val TITLE_SLOT = 2
    private const val SUBTITLE_SLOT = 4
    private const val PACING_SLOT = 6
    private const val FADE_IN_SLOT = 10
    private const val STAY_SLOT = 12
    private const val FADE_OUT_SLOT = 14
    private const val REMOVE_SLOT = 16

    private val DEFAULTS = TitleConfiguration()

    private fun IntRange.toDoubleRange(): ClosedFloatingPointRange<Double> = first.toDouble()..last.toDouble()

    private fun ticksLine(ticks: Double) =
        "<val>${NpcFormat.seconds(ticks.toInt(), 2)} <hnt>· ${ticks.toInt()} ticks"

    private val FADE_IN = ticksStepper("Fade In", fadeRange, 1.0, DEFAULTS.fadeIn)
    private val STAY = ticksStepper("Stay", stayRange, 5.0, DEFAULTS.stay)
    private val FADE_OUT = ticksStepper("Fade Out", fadeRange, 1.0, DEFAULTS.fadeOut)

    private fun ticksStepper(label: String, range: ClosedFloatingPointRange<Double>, step: Double, default: Int) =
        Stepper(label, range, step, default.toDouble(), 0, bigMultiplier = 5, valueLine = ::ticksLine)

    class Port(
        val player: NpcPlayer,
        val title: TitleConfiguration?,
        val textPrompts: TextPrompts,
        val edit: ((TitleConfiguration) -> TitleConfiguration) -> Unit,
        val removeAndBack: () -> Unit,
    )

    fun normalized(title: TitleConfiguration): TitleConfiguration? =
        title.takeIf { it.title.isNotBlank() || it.subtitle.isNotBlank() }

    fun defaults(): TitleConfiguration = DEFAULTS

    fun fill(pane: Pane, port: Port) {
        val title = port.title ?: DEFAULTS

        fun ask(label: String, value: String, set: (TitleConfiguration, String) -> TitleConfiguration) {
            port.textPrompts.open(
                port.player,
                title = label,
                instruction = "Type the ${label.lowercase()} in chat.",
                current = value,
                format = PromptFormat.MINI_MESSAGE,
                placeholders = PromptTokens.PLAYER,
                onClear = { port.edit { set(it, "") } }.takeIf { value.isNotBlank() },
            ) { text ->
                port.edit { set(it, text) }
            }
        }

        pane.left(TITLE_SLOT, textField("Title Text", title.title)) {
            ask("Title Text", title.title) { current, text -> current.copy(title = text) }
        }
        pane.left(SUBTITLE_SLOT, textField("Subtitle Text", title.subtitle)) {
            ask("Subtitle Text", title.subtitle) { current, text -> current.copy(subtitle = text) }
        }

        val total = title.fadeIn + title.stay + title.fadeOut
        pane[PACING_SLOT] = Ui.item(
            "CLOCK",
            "<ttl>Pacing",
            listOf(
                NpcFormat.pacing(title),
                "<bd>On screen for <val>${NpcFormat.seconds(total)}",
            ),
        )

        if (port.title == null) {
            val hint = listOf("<hnt>Set a title or subtitle first.")
            listOf(FADE_IN_SLOT to FADE_IN, STAY_SLOT to STAY, FADE_OUT_SLOT to FADE_OUT).forEach { (slot, stepper) ->
                pane[slot] = Ui.disabled(stepper.label, hint)
            }
        } else {
            pane[FADE_IN_SLOT] = FADE_IN.element(title.fadeIn.toDouble()) { value ->
                port.edit { it.copy(fadeIn = value.toInt()) }
            }
            pane[STAY_SLOT] = STAY.element(title.stay.toDouble()) { value ->
                port.edit { it.copy(stay = value.toInt()) }
            }
            pane[FADE_OUT_SLOT] = FADE_OUT.element(title.fadeOut.toDouble()) { value ->
                port.edit { it.copy(fadeOut = value.toInt()) }
            }
        }

        pane.left(
            REMOVE_SLOT,
            Ui.item(
                "RED_CONCRETE",
                "<err>Remove Title",
                listOf("<hnt>Other parts of the action stay.", "<key>Left <hnt>Remove"),
            ),
        ) { port.removeAndBack() }
    }

    private fun textField(label: String, value: String): NpcItem = Ui.item(
        "ANVIL",
        "<ttl>$label",
        listOf(
            if (value.isBlank()) "<off>Not set" else "<val>${Ui.quote(value)}",
            "<key>Left <hnt>Edit in chat",
        ),
    )
}
