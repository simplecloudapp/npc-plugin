package app.simplecloud.npc.common.editor.inventory.canvas

import app.simplecloud.npc.common.editor.canvas.CanvasView
import app.simplecloud.npc.common.editor.inventory.CanvasMode
import app.simplecloud.npc.common.editor.inventory.InventoryEditorSession
import app.simplecloud.npc.common.editor.inventory.LiveGroups
import app.simplecloud.npc.common.editor.ui.Palette
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.inventory.view.ItemIconResolver
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.PageControl

object CanvasRender {

    private const val MAX_CARRIED = 99

    class LiveState(val slots: Map<Int, NpcItem>, val loading: Boolean, val cloudDown: Boolean)

    fun view(config: InventoryConfiguration, session: InventoryEditorSession): CanvasView {
        val live = session.live
        val items = if (session.mode == CanvasMode.LIVE && live != null) live.slots else designItems(config, session)

        return CanvasView(
            title = config.title,
            size = config.size(),
            items = items,
            toolbox = toolbox(config, session, live),
            carried = session.carried?.let { carriedIcon(config, it.item) },
            showInventory = session.handMode,
        )
    }

    fun designItems(config: InventoryConfiguration, session: InventoryEditorSession): Map<Int, NpcItem> {
        val selection = session.selectionFor(config.id)
        val lifted = session.carried?.from
        val positions = LiveGroups.names(config).associateWith { LiveGroups.contentSlots(config, it) }

        return config.items.filter { it.slot in 0 until config.size() }.associate { item ->
            if (item.slot == lifted) return@associate item.slot to liftedMarker()

            val base = ItemIconResolver.icon(item)
            val icon = item.liveGroup?.let { group ->
                val (_, hex) = LiveGroups.colorOf(config, group)
                val label = base.name.ifBlank { Ui.pretty(item.material) }
                when (item.pageControl) {
                    PageControl.PREVIOUS -> base.copy(name = "<$hex>◀ <white>$label")
                    PageControl.NEXT -> base.copy(name = "<$hex>▶ <white>$label")
                    else -> base.copy(
                        name = "<$hex>● <white>$label",
                        amount = ((positions[group]?.indexOf(item.slot) ?: 0) + 1).coerceIn(1, 64),
                    )
                }
            } ?: base

            val selected = item.slot in selection
            item.slot to icon.copy(
                glowing = icon.glowing || selected,
                hideTooltip = false,
                lore = icon.lore + footer(item, selected),
            )
        }
    }

    private fun footer(item: InventoryItemConfiguration, selected: Boolean): List<String> = listOfNotNull(
        "",
        item.liveGroup?.let { group ->
            if (item.pageControl != null) {
                "<hnt>Page ${item.pageControl} of <val>$group"
            } else {
                "<hnt>Live slot of <val>$group"
            }
        },
        "<info>● Selected".takeIf { selected },
        "<hnt>Slot ${item.slot} · <key>Left <hnt>lift · <key>Right <hnt>Studio · <key>Q <hnt>delete",
    ).map(Palette::expand)

    private fun liftedMarker(): NpcItem = Ui.item(
        "LIGHT_GRAY_STAINED_GLASS_PANE",
        "<hnt>Lifted from here",
        listOf("<hnt>Drop it anywhere, or press <key>F <hnt>to put it back."),
    )

    private fun carriedIcon(config: InventoryConfiguration, item: InventoryItemConfiguration): NpcItem {
        val free = (0 until config.size()).count { slot -> config.items.none { it.slot == slot } }
        val icon = ItemIconResolver.icon(item)

        return icon.copy(
            name = icon.name.ifBlank { Ui.pretty(item.material) },
            amount = free.coerceIn(1, MAX_CARRIED),
            maxStackSize = MAX_CARRIED,
            hideTooltip = false,
        )
    }

    fun toolbox(config: InventoryConfiguration, session: InventoryEditorSession, live: LiveState?): Map<Int, NpcItem> {
        val history = session.history(config.id)
        val selection = session.selectionFor(config.id)
        val tools = mutableMapOf<Int, NpcItem>()

        tools[Toolbox.HUB] = Ui.back().copy(lore = listOf(Palette.expand("<hnt>To the menu's hub. Esc does the same.")))
        tools[Toolbox.UNDO] = Ui.item(
            if (history.canUndo) "CLOCK" else "GRAY_DYE",
            "<ttl>Undo",
            listOf("<bd><val>${history.undoCount} <bd>steps", "", "<key>Left <hnt>Undo the last change"),
        )
        tools[Toolbox.REDO] = Ui.item(
            if (history.canRedo) "CLOCK" else "GRAY_DYE",
            "<ttl>Redo",
            listOf("<bd><val>${history.redoCount} <bd>steps", "", "<key>Left <hnt>Redo"),
        )
        tools[Toolbox.MODE] = if (session.mode == CanvasMode.LIVE) {
            Ui.item(
                "SPYGLASS",
                "<ttl>Live Preview <on>ON",
                listOfNotNull(
                    "<bd>What players see right now.",
                    when {
                        live == null || live.loading -> "<hnt>Loading the cloud data..."
                        live.cloudDown -> "<warn>The cloud is unreachable; live slots stay empty."
                        else -> null
                    },
                    "<hnt>Page arrows turn pages here.",
                    "",
                    "<key>Left <hnt>Back to Design",
                ),
                glowing = true,
            )
        } else {
            Ui.item(
                "ENDER_EYE",
                "<ttl>Design",
                listOf("<bd>Templates as you edit them.", "", "<key>Left <hnt>Show the live preview"),
            )
        }
        tools[Toolbox.TITLE] = Ui.item(
            "NAME_TAG",
            "<ttl>Title",
            listOf("<bd>Now: " + config.title, "", "<key>Left <hnt>Type a new one in chat"),
        )
        tools[Toolbox.ROWS] = Ui.item(
            "LADDER",
            "<ttl>Rows <val>${config.rows}",
            listOf(
                "<key>Left <hnt>Add a row  <key>Right <hnt>Remove the bottom row",
                "<key>Shift+Left <hnt>Move ${if (selection.isEmpty()) "everything" else "the selection"} down",
                "<key>Shift+Right <hnt>Move it up",
            ),
        )
        tools[Toolbox.FILL] = Ui.item(
            Toolbox.DECOR_MATERIAL,
            "<ttl>Fill Empty Slots",
            listOf("<bd>With what you carry, or a gray pane.", "", "<key>Left <hnt>Fill"),
        )
        tools[Toolbox.BORDER] = Ui.item(
            "BLACK_STAINED_GLASS_PANE",
            "<ttl>Border",
            listOf(
                "<bd>Frames the empty edge slots with what",
                "<bd>you carry, or a gray pane.",
                "",
                "<key>Left <hnt>Draw",
            ),
        )
        tools[Toolbox.HELP] = Ui.item("BOOK", "<ttl>Controls", HELP)

        session.recentMaterials.zip(Toolbox.RECENT.toList()).forEach { (material, index) ->
            tools[index] = Ui.item(
                material,
                "<ttl>${Ui.pretty(material)}",
                listOf("<hnt>Recently used", "", "<key>Left <hnt>Pick up a new one"),
            )
        }
        tools[Toolbox.PICK] =
            Ui.item("CHEST", "<ttl>Pick A Material...", listOf("<bd>Search every material.", "", "<key>Left <hnt>Open"))
        tools[Toolbox.HAND] = Ui.item(
            "LEATHER",
            "<ttl>From Your Inventory",
            listOf(
                "<bd>Shows your own items here; click one",
                "<bd>to copy its look onto the cursor.",
                "<hnt>Styled with an anvil or another plugin? Works.",
                "",
                "<key>Left <hnt>Show my items",
            ),
        )

        LiveGroups.names(config).zip(Toolbox.SWATCHES.toList()).forEach { (group, index) ->
            tools[index] = swatch(config, session, group)
        }
        tools[Toolbox.NEW_GROUP] = Ui.addHead(
            "New Live Group",
            listOf("<bd>Servers, groups, ... that fill", "<bd>slots on their own.", "", "<key>Left <hnt>Pick a source"),
        )
        val active = session.activeGroup?.takeIf { it in LiveGroups.names(config) }
        tools[Toolbox.PREVIOUS_ARROW] = arrow("Previous Page", active)
        tools[Toolbox.NEXT_ARROW] = arrow("Next Page", active)

        tools[Toolbox.HOTBAR + Toolbox.KEY_STAMP] = key(
            1,
            "PAPER",
            "Stamp",
            session.lastPlaced?.let { "<bd>Places <val>${Ui.pretty(it.material)}" }
                ?: "<hnt>Places the last item you dropped.",
        )
        tools[Toolbox.HOTBAR + Toolbox.KEY_ERASE] =
            key(2, "BARRIER", "Erase", "<bd>Deletes the item (or the selection).")
        tools[Toolbox.HOTBAR + Toolbox.KEY_COPY] = key(3, "SHEARS", "Copy", "<bd>Puts a copy on your cursor.")
        tools[Toolbox.HOTBAR + Toolbox.KEY_LIVE] = key(
            4,
            "LIME_STAINED_GLASS_PANE",
            "Live",
            active?.let { "<bd>Adds a slot to <val>$it" } ?: "<hnt>Pick a live group first.",
        )
        tools[Toolbox.HOTBAR + Toolbox.KEY_DECOR] =
            key(5, Toolbox.DECOR_MATERIAL, "Decor", "<bd>Places a gray pane without tooltip.")
        tools[Toolbox.HOTBAR + Toolbox.KEY_STUDIO] = key(6, "SMITHING_TABLE", "Studio", "<bd>Opens the Item Studio.")
        Toolbox.KEY_BINDINGS.forEach { keyIndex ->
            val bound = session.keyBindings[keyIndex]
            tools[Toolbox.HOTBAR + keyIndex] = if (bound == null) {
                key(
                    keyIndex + 1,
                    "LIGHT_GRAY_STAINED_GLASS_PANE",
                    "Free Key",
                    "<hnt>Carry an item and press ${keyIndex + 1} to bind it.",
                )
            } else {
                val icon = ItemIconResolver.icon(bound)
                val label = icon.name.ifBlank { Ui.pretty(bound.material) }
                icon.copy(
                    name = Palette.expand("<key>${keyIndex + 1} <ttl>") + label,
                    lore = listOf(
                        "<bd>Hover a slot and press ${keyIndex + 1}",
                        "<bd>to place it.",
                    ).map(Palette::expand),
                    hideTooltip = false,
                )
            }
        }

        return tools
    }

    private fun swatch(config: InventoryConfiguration, session: InventoryEditorSession, group: String): NpcItem {
        val (color, _) = LiveGroups.colorOf(config, group)
        val source = LiveGroups.source(config, group)
        val slots = LiveGroups.contentSlots(config, group).size
        val active = session.activeGroup == group
        val sourceLabel = source?.let { LiveGroups.describe(it.type).label } ?: "<err>none"
        val arrowsMissing = LiveGroups.lacksPageControls(config, group)

        return Ui.item(
            "${color}_STAINED_GLASS_PANE",
            "<ttl>$group",
            listOfNotNull(
                "<bd>Source <val>$sourceLabel",
                "<bd><val>$slots <bd>slots",
                "<warn>No page arrows: extra entries are hidden.".takeIf { arrowsMissing },
                "<on>Active: arrows and key 4 use it".takeIf { active },
                "",
                "<key>Left <hnt>Pick up a slot to paint",
                "<key>Right <hnt>Group settings",
            ),
            glowing = active,
            amount = slots.coerceIn(1, 64),
        )
    }

    private fun arrow(label: String, group: String?): NpcItem = Ui.item(
        "ARROW",
        "<ttl>$label",
        if (group == null) listOf("<hnt>Pick a live group first.") else listOf(
            "<bd>Turns the pages of <val>$group",
            "",
            "<key>Left <hnt>Pick up",
        ),
    )

    private fun key(number: Int, material: String, label: String, line: String): NpcItem =
        Ui.item(material, "<key>$number <ttl>$label", listOf(line, "", "<hnt>Hover a slot above and press $number."))

    private val HELP = listOf(
        "<key>Left <hnt>lift an item, click again to drop it",
        "<key>Right <hnt>while carrying stamps a copy",
        "<key>Drag <hnt>while carrying paints every slot",
        "<key>Right <hnt>on an item opens the Item Studio",
        "<key>Shift+Left <hnt>select  <key>Shift+Right <hnt>select alike",
        "<key>Q <hnt>delete  <key>F <hnt>copy / put back",
        "<key>1-6 <hnt>hotbar tools  <key>7-9 <hnt>your own items",
        "<hnt>Click outside the window to throw away.",
        "<hnt>Every change saves at once; Undo is above.",
    )
}
