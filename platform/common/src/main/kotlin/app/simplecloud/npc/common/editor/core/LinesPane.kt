package app.simplecloud.npc.common.editor.core

import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.ui.Palette
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.common.text.Msg

class LineList<T>(
    val lines: List<T>,
    val textOf: (T) -> String,
    val withText: (T, String) -> T,
    val create: (String) -> T,
    val edit: ((MutableList<T>) -> Boolean) -> Unit,
    val prompt: (index: Int?, current: String?, onText: (String) -> Unit) -> Unit,
    val hints: (T) -> List<String> = { emptyList() },
    val secondary: LineAction? = null,
)

class LineAction(val label: String, val onClick: (index: Int) -> Unit)

object LinesPane {
    const val SIZE = 36

    fun <T> place(
        pane: Pane,
        list: LineList<T>,
        controlsSlot: Int,
        note: String? = null,
        addLabel: String = "Add Line",
    ) {
        val lines = list.lines
        val max = pane.size - 9
        val addSlot = pane.size - 1
        val full = lines.size >= max

        lines.take(max).forEachIndexed { index, line ->
            val text = list.textOf(line)
            val item = lineItem(text, index, lines.size, list.hints(line), list.secondary?.label ?: "Duplicate")
            pane.on(index, item) { click ->
                when (click) {
                    MenuClick.LEFT -> list.prompt(index, text) { typed ->
                        list.edit { fresh ->
                            if (index !in fresh.indices) return@edit false
                            fresh[index] = list.withText(fresh[index], typed)
                            true
                        }
                    }

                    MenuClick.RIGHT -> list.secondary?.let { it.onClick(index) } ?: duplicate(list, index, max)

                    MenuClick.SHIFT_LEFT -> move(list, index, -1)
                    MenuClick.SHIFT_RIGHT -> move(list, index, +1)
                    MenuClick.DROP -> list.edit { fresh ->
                        if (index !in fresh.indices) return@edit false
                        fresh.removeAt(index)
                        true
                    }

                    else -> Unit
                }
            }
        }

        if (full) {
            pane[addSlot] = Ui.disabled(addLabel, listOf("<hnt>All $max are in use."))
        } else {
            pane.left(addSlot, Ui.addHead(addLabel, listOf("", "<key>Left <hnt>Type the text in chat"))) {
                list.prompt(null, null) { typed ->
                    list.edit { fresh ->
                        if (fresh.size >= max) return@edit false
                        fresh += list.create(typed)
                        true
                    }
                }
            }
        }

        pane[controlsSlot] = Ui.item(
            "BOOK",
            "<ttl>Controls",
            listOfNotNull(
                "<key>Left <hnt>Edit this line in chat",
                "<key>Right <hnt>${list.secondary?.label ?: "Duplicate below"}",
                "<key>Shift+Left <hnt>Move up  <key>Shift+Right <hnt>Move down",
                "<key>Q <err>Delete, no confirmation",
                note,
            ),
        )
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
    ): NpcItem = NpcItem(
        "PAPER",
        text.ifBlank { Palette.expand("<hnt>(blank line)") },
        listOf(
            Palette.expand("<hnt>Line ${index + 1} of $count"),
            Palette.expand("<hnt>raw: ") + Msg.miniMessage.escapeTags(text.ifBlank { "(empty)" }),
        ) + (hints + listOf(
            "",
            "<key>Left <hnt>Edit  <key>Right <hnt>$rightLabel",
            "<key>Shift+Left <hnt>Move up  <key>Shift+Right <hnt>Move down",
            "<key>Q <err>Delete line",
        )).map(Palette::expand),
    )
}
