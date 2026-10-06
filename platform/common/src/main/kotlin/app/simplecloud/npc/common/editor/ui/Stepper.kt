package app.simplecloud.npc.common.editor.ui

import app.simplecloud.npc.common.editor.menu.Element
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.item.NpcItem
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

data class Stepper(
    val label: String,
    val range: ClosedFloatingPointRange<Double>,
    val step: Double,
    val default: Double,
    val decimals: Int,
    val material: String = "COMPARATOR",
    val unit: String? = null,
    val hints: List<String> = emptyList(),
    val bigMultiplier: Int = 10,
    val valueLine: ((Double) -> String)? = null,
) {
    val min: Double get() = range.start
    val max: Double get() = range.endInclusive

    fun fmt(value: Double): String =
        if (decimals == 0) value.roundToInt().toString() else String.format(Locale.ROOT, "%.${decimals}f", value)

    fun round(value: Double): Double {
        val factor = 10.0.pow(decimals)
        return (value * factor).roundToInt() / factor
    }

    fun item(value: Double): NpcItem {
        val valueLine = valueLine?.invoke(value) ?: "<val>${fmt(value)}" + unit?.let { " <hnt>$it" }.orEmpty()
        return Ui.item(
            material,
            "<ttl>$label",
            listOf("$valueLine <hnt>(${fmt(min)} – ${fmt(max)})") + hints + listOf(
                "<key>Left <hnt>+${fmt(step)} · <key>Right <hnt>-${fmt(step)} · <key>Shift <hnt>x$bigMultiplier",
            ),
        )
    }

    fun element(current: Double, commit: (Double) -> Unit): Element =
        Element(item(current)) { click -> stepped(current, click)?.let(commit) }

    fun stepped(current: Double, click: MenuClick): Double? = when (click) {
        MenuClick.LEFT, MenuClick.RIGHT, MenuClick.SHIFT_LEFT, MenuClick.SHIFT_RIGHT ->
            round((current + click.step(step, step * bigMultiplier)).coerceIn(min, max))

        else -> null
    }
}
