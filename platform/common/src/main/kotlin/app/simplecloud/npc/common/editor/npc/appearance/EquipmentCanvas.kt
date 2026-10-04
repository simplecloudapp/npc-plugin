package app.simplecloud.npc.common.editor.npc.appearance

import app.simplecloud.npc.common.editor.canvas.CanvasInput
import app.simplecloud.npc.common.editor.canvas.CanvasSurface
import app.simplecloud.npc.common.editor.canvas.CanvasView
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.npc.NpcEditorContext
import app.simplecloud.npc.common.editor.npc.NpcEditorScreen
import app.simplecloud.npc.common.editor.npc.NpcEditorSession
import app.simplecloud.npc.common.editor.npc.NpcFormat
import app.simplecloud.npc.common.editor.ui.Palette
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.core.config.EquipmentSlot
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.config.NpcConfig.EquipmentConfiguration
import app.simplecloud.npc.core.config.NpcConfig.EquipmentItem
import app.simplecloud.npc.core.platform.NpcPlayer

class EquipmentCanvas(
    private val context: NpcEditorContext,
    private val surface: CanvasSurface,
    private val onEscape: (NpcPlayer) -> Unit,
) {

    fun show(player: NpcPlayer, screen: NpcEditorScreen.Equipment) {
        val config = context.npcRepository.find(screen.npcId) ?: run {
            context.reportMissing(player, screen.npcId)
            context.close(player)
            return
        }
        val session = context.session(player)

        surface.open(
            player,
            view(config, session),
            onInput = { input -> handle(player, screen, input) },
            onClose = { onEscape(player) },
        )
    }

    private fun handle(player: NpcPlayer, screen: NpcEditorScreen.Equipment, input: CanvasInput) {
        val session = context.session(player)
        val config = context.npcRepository.find(screen.npcId) ?: return

        when (input) {
            is CanvasInput.Click -> when {
                input.slot >= SIZE -> fromInventory(player, screen, session, input.slot - SIZE, input.shift)
                else -> SLOT_AT[input.slot]?.let { slot -> clickSlot(player, screen, session, config, slot, input) }
                    ?: button(player, screen, session, config, input.slot)
            }

            is CanvasInput.Key -> SLOT_AT[input.slot]?.let { slot ->
                val index = if (input.key == CanvasInput.OFFHAND_KEY) input.key else HOTBAR_START + input.key
                surface.equipmentItem(player, index)?.let { put(player, screen, slot, it) }
            }

            is CanvasInput.Drag -> session.carriedEquipment?.let { carried ->
                val slots = input.slots.mapNotNull(SLOT_AT::get)
                if (slots.isEmpty()) return
                session.carriedEquipment = null
                edit(player, screen) { equipment -> slots.fold(equipment) { acc, slot -> acc.with(slot, carried) } }
            }

            is CanvasInput.Drop -> SLOT_AT[input.slot]?.let { slot -> put(player, screen, slot, null) }

            is CanvasInput.Outside -> if (session.carriedEquipment != null) {
                session.carriedEquipment = null
                context.render(player, screen)
            }

            is CanvasInput.DoubleClick, is CanvasInput.Clone -> Unit
        }
    }

    private fun fromInventory(
        player: NpcPlayer,
        screen: NpcEditorScreen.Equipment,
        session: NpcEditorSession,
        index: Int,
        shift: Boolean,
    ) {
        val look = surface.equipmentItem(player, index) ?: run {
            if (session.carriedEquipment != null) {
                session.carriedEquipment = null
                context.render(player, screen)
            }
            return
        }

        if (shift) {
            put(player, screen, EquipmentSlot.suitedFor(look.material), look)
        } else {
            session.carriedEquipment = look
            context.render(player, screen)
        }
    }

    private fun clickSlot(
        player: NpcPlayer,
        screen: NpcEditorScreen.Equipment,
        session: NpcEditorSession,
        config: NpcConfig,
        slot: EquipmentSlot,
        click: CanvasInput.Click,
    ) {
        val worn = config.entity.equipment.of(slot)
        val carried = session.carriedEquipment

        when {
            click.shift -> put(player, screen, slot, null)
            carried != null -> {
                session.carriedEquipment = worn
                put(player, screen, slot, carried)
            }

            click.right -> worn?.let { put(player, screen, slot, it.copy(glowing = !it.glowing)) }
            worn != null -> {
                session.carriedEquipment = worn
                put(player, screen, slot, null)
            }
        }
    }

    private fun button(
        player: NpcPlayer,
        screen: NpcEditorScreen.Equipment,
        session: NpcEditorSession,
        config: NpcConfig,
        slot: Int,
    ) {
        when (slot) {
            BACK_SLOT -> context.back(player)
            COPY_SLOT -> {
                val worn = player.wornArmor()
                edit(player, screen) { equipment ->
                    ARMOR.fold(equipment) { acc, armorSlot -> acc.with(armorSlot, worn.of(armorSlot)) }
                }
            }

            CLEAR_SLOT -> if (config.entity.equipment.filled().isNotEmpty() || session.carriedEquipment != null) {
                session.carriedEquipment = null
                edit(player, screen) { EquipmentConfiguration() }
            }
        }
    }

    private fun put(player: NpcPlayer, screen: NpcEditorScreen.Equipment, slot: EquipmentSlot, item: EquipmentItem?) =
        edit(player, screen) { it.with(slot, item) }

    private fun edit(
        player: NpcPlayer,
        screen: NpcEditorScreen.Equipment,
        mutate: (EquipmentConfiguration) -> EquipmentConfiguration,
    ) {
        context.editEntity(player, screen.npcId, screen) { it.copy(equipment = mutate(it.equipment)) }
    }

    private fun view(config: NpcConfig, session: NpcEditorSession): CanvasView {
        val equipment = config.entity.equipment
        val items = mutableMapOf<Int, NpcItem>()

        (0 until SIZE).forEach { items[it] = BACKGROUND }
        NAV_ROW.forEach { items[it] = NAV_FILLER }

        items[HEADER_SLOT] = Ui.item(
            "ARMOR_STAND",
            "<ttl>Equipment",
            listOf(
                "<bd><val>${equipment.filled().size} <bd>of <val>${EquipmentSlot.entries.size} <bd>slots worn",
                "",
                "<hnt>Pick up any item from your inventory",
                "<hnt>below and drop it on the NPC.",
                "<key>Shift+Left <hnt>on your item puts it where it fits.",
            ),
        )
        SLOTS.forEach { (slot, menuSlot) -> items[menuSlot] = slotIcon(slot, equipment.of(slot)) }

        items[BACK_SLOT] = Ui.back()
        items[COPY_SLOT] = Ui.item(
            "LEATHER_CHESTPLATE",
            "<ttl>Copy My Armor",
            listOf("<bd>Dresses the NPC in the armor", "<bd>you are wearing right now.", "", "<key>Left <hnt>Copy"),
        )
        items[CLEAR_SLOT] = if (equipment.filled().isEmpty()) {
            Ui.disabled("Take Everything Off", listOf("<hnt>The NPC wears nothing."))
        } else {
            Ui.item("BUCKET", "<err>Take Everything Off", listOf("", "<key>Left <hnt>Clear all slots"))
        }
        items[HELP_SLOT] = Ui.item("BOOK", "<ttl>Controls", HELP)

        return CanvasView(
            title = Ui.title("Equipment", NpcFormat.displayName(config)),
            size = SIZE,
            items = items,
            carried = session.carriedEquipment?.let { icon(it, "<ttl>${Ui.pretty(it.material)}", emptyList()) },
            showInventory = true,
        )
    }

    private fun slotIcon(slot: EquipmentSlot, worn: EquipmentItem?): NpcItem {
        val label = label(slot)
        worn ?: return Ui.item(
            "LIGHT_GRAY_STAINED_GLASS_PANE",
            "<ttl>$label",
            listOf(
                "<off>Empty",
                "",
                "<hnt>Drop an item from your inventory here",
            ),
        )

        val details = listOfNotNull(
            "<bd>Wears <val>${Ui.pretty(worn.material)}",
            worn.color?.let { "<bd>Color <val>$it" },
            worn.trim?.let { "<bd>Trim <val>${pretty(it.pattern)} <bd>in <val>${pretty(it.material)}" },
            worn.customModelData?.let { "<bd>Model data <val>$it" },
        )

        return icon(
            worn,
            "<ttl>$label",
            details + listOf(
                "",
                "<key>Left <hnt>Pick it up  <key>Right <hnt>Glint ${if (worn.glowing) "off" else "on"}",
                "<key>Shift+Left <hnt>or <key>Q <hnt>Take it off",
            ),
        )
    }

    private fun icon(item: EquipmentItem, name: String, lore: List<String>): NpcItem = NpcItem(
        material = item.material,
        name = Palette.expand(name),
        lore = lore.map(Palette::expand),
        glowing = item.glowing,
        skinTexture = item.headTexture,
        customModelData = item.customModelData,
        armorColor = item.color,
        armorTrim = item.trim,
    )

    private fun pretty(key: String): String = Ui.pretty(key.substringAfter(':').uppercase())

    private fun label(slot: EquipmentSlot): String = when (slot) {
        EquipmentSlot.MAIN_HAND -> "Main Hand"
        EquipmentSlot.OFF_HAND -> "Off Hand"
        EquipmentSlot.HELMET -> "Helmet"
        EquipmentSlot.CHESTPLATE -> "Chestplate"
        EquipmentSlot.LEGGINGS -> "Leggings"
        EquipmentSlot.BOOTS -> "Boots"
    }

    private companion object {
        const val SIZE = 54
        const val HOTBAR_START = 27
        const val HEADER_SLOT = 4
        const val BACK_SLOT = 45
        const val COPY_SLOT = 48
        const val CLEAR_SLOT = 50
        const val HELP_SLOT = 53
        val NAV_ROW = 45 until SIZE

        val SLOTS = mapOf(
            EquipmentSlot.HELMET to 13,
            EquipmentSlot.MAIN_HAND to 21,
            EquipmentSlot.CHESTPLATE to 22,
            EquipmentSlot.OFF_HAND to 23,
            EquipmentSlot.LEGGINGS to 31,
            EquipmentSlot.BOOTS to 40,
        )
        val SLOT_AT = SLOTS.entries.associate { (slot, menuSlot) -> menuSlot to slot }
        val ARMOR = listOf(EquipmentSlot.HELMET, EquipmentSlot.CHESTPLATE, EquipmentSlot.LEGGINGS, EquipmentSlot.BOOTS)

        val BACKGROUND = NpcItem("BLACK_STAINED_GLASS_PANE", " ", hideTooltip = true)
        val NAV_FILLER = NpcItem(Pane.GRAY_PANE, " ", hideTooltip = true)

        val HELP = listOf(
            "<key>Left <hnt>on your item picks up a copy",
            "<key>Left <hnt>on a slot drops it there",
            "<key>Shift+Left <hnt>on your item puts it where it fits",
            "<key>1-9 <hnt>/ <key>F <hnt>on a slot takes that hotbar / off-hand item",
            "<key>Right <hnt>on a slot toggles the glint",
            "<key>Q <hnt>takes it off",
            "<hnt>Your own items never move.",
        )
    }
}
