package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.ui.Palette
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.item.NpcItem

class LineList<T>(
    val lines: List<T>,
    val textOf: (T) -> String,
    val withText: (T, String) -> T,
    val create: (String) -> T,
    val edit: ((MutableList<T>) -> Boolean) -> Unit,
    val prompt: (index: Int?, current: String?, onText: (String) -> Unit, onDelete: (() -> Unit)?) -> Unit,
    val hints: (T) -> List<String> = { emptyList() },
    val secondary: LineAction? = null,
    val opensSecondary: (T) -> Boolean = { false },
)

class LineAction(val label: String, val onClick: (index: Int) -> Unit)

object LinesPane {
    const val SIZE = 36

    fun <T> place(pane: Pane, list: LineList<T>, slots: List<Int>, addLabel: String = "Add Line") {
        val lines = list.lines
        val max = slots.size

        lines.take(max).forEachIndexed { index, line ->
            val text = list.textOf(line)
            val secondary = list.secondary?.takeIf { list.opensSecondary(line) }
            val rightLabel = list.secondary?.label ?: "Duplicate"
            val item = lineItem(text, index, lines.size, list.hints(line), rightLabel, secondary)
            pane.on(slots[index], item) { click ->
                when (click) {
                    MenuClick.LEFT -> secondary?.onClick(index) ?: editText(list, index, text)
                    MenuClick.RIGHT -> list.secondary?.let { it.onClick(index) } ?: duplicate(list, index, max)
                    MenuClick.SHIFT_LEFT -> move(list, index, -1)
                    MenuClick.SHIFT_RIGHT -> move(list, index, +1)
                    else -> Unit
                }
            }
        }

        if (lines.size < max) {
            pane.left(slots[lines.size], Ui.addHead(addLabel, listOf("<key>Left <hnt>Type the text in chat"))) {
                list.prompt(null, null, { typed ->
                    list.edit { fresh ->
                        if (fresh.size >= max) return@edit false
                        fresh += list.create(typed)
                        true
                    }
                }, null)
            }
        }
    }

    private fun <T> editText(list: LineList<T>, index: Int, text: String) {
        list.prompt(index, text, { typed ->
            list.edit { fresh ->
                if (index !in fresh.indices) return@edit false
                fresh[index] = list.withText(fresh[index], typed)
                true
            }
        }) {
            list.edit { fresh ->
                if (index !in fresh.indices) return@edit false
                fresh.removeAt(index)
                true
            }
        }
    }

    private fun <T> duplicate(list: LineList<T>, index: Int, max: Int) = list.edit { fresh ->
        if (fresh.size >= max || index !in fresh.indices) return@edit false
        fresh.add(index + 1, fresh[index])
        true
    }

    private fun <T> move(list: LineList<T>, index: Int, direction: Int) = list.edit { fresh ->
        val target = index + direction
        if (index !in fresh.indices || target !in fresh.indices) return@edit false
        fresh.add(target, fresh.removeAt(index))
        true
    }

    private fun lineItem(
        text: String,
        index: Int,
        count: Int,
        hints: List<String>,
        rightLabel: String,
        leftOpens: LineAction?,
    ): NpcItem {
        val clicks = leftOpens?.let { "<key>Left <hnt>${it.label} · <key>Shift <hnt>Move" }
            ?: "<key>Left <hnt>Edit · <key>Right <hnt>$rightLabel · <key>Shift <hnt>Move"

        return NpcItem(
            "PAPER",
            text.ifBlank { Palette.expand("<hnt>(blank line)") },
            (listOf("<hnt>Line ${index + 1} of $count") + hints + clicks).map(Palette::expand),
        )
    }
}
