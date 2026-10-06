package app.simplecloud.npc.common.editor.inventory.screens

import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.PromptPlaceholder
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.common.editor.PromptTokens
import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.core.toggle
import app.simplecloud.npc.common.editor.inventory.CanvasMode
import app.simplecloud.npc.common.editor.inventory.InventoryEditorContext
import app.simplecloud.npc.common.editor.inventory.InventoryEditorScreen
import app.simplecloud.npc.common.editor.inventory.InventoryPickerPurpose
import app.simplecloud.npc.common.editor.inventory.LiveGroups
import app.simplecloud.npc.common.editor.inventory.StudioTab
import app.simplecloud.npc.common.editor.inventory.StudioTargets
import app.simplecloud.npc.common.editor.inventory.StudioTargets.Shared
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.ui.Clicks
import app.simplecloud.npc.common.editor.ui.Palette
import app.simplecloud.npc.common.editor.ui.Stepper
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.inventory.ItemClickActions
import app.simplecloud.npc.common.inventory.view.HeadOwners
import app.simplecloud.npc.common.inventory.view.ItemIconResolver
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.common.utils.SkinInput
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.HeadConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer
import net.kyori.adventure.text.Component

object StudioMenuBuilder {
    private const val SIZE = 45

    private const val LOOK_TAB = 1
    private const val CLICK_TAB = 2
    private const val LIVE_TAB = 3
    private const val PREVIEW_SLOT = 6
    private val HEADER_FILLER = listOf(0, 4, 5, 7, 8)

    private const val MATERIAL_SLOT = 19
    private const val NAME_SLOT = 20
    private const val LORE_SLOT = 21
    private const val AMOUNT_SLOT = 22
    private const val GLINT_SLOT = 23
    private const val TOOLTIP_SLOT = 24
    private const val MODEL_SLOT = 25
    private const val HEAD_SLOT = 31

    private val CLICK_SLOTS = mapOf(
        PlayerInteraction.LEFT_CLICK to 20,
        PlayerInteraction.RIGHT_CLICK to 21,
        PlayerInteraction.SHIFT_LEFT_CLICK to 23,
        PlayerInteraction.SHIFT_RIGHT_CLICK to 24,
    )
    private const val CLICK_INFO_SLOT = 31

    private const val GROUP_SLOT = 20
    private const val SCOPE_SLOT = 22
    private const val PLACEHOLDERS_SLOT = 24

    private const val COUNT_SLOT = 40
    private const val DELETE_SLOT = 44

    private val AMOUNT = Stepper(
        label = "Amount",
        range = 1.0..64.0,
        step = 1.0,
        default = 1.0,
        decimals = 0,
        material = "SLIME_BALL",
        bigMultiplier = 8,
    )
    private val MODEL_DATA = Stepper(
        label = "Custom Model Data",
        range = 0.0..9_999_999.0,
        step = 1.0,
        default = 0.0,
        decimals = 0,
        material = "ITEM_FRAME",
        hints = listOf("<hnt>0 means none; for resource packs."),
        bigMultiplier = 100,
    )

    fun build(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.Studio,
    ): EditorMenu {
        val session = context.session(player)
        val targets = StudioTargets.of(config, screen.slots, session.thisSlotOnly)
        val items = StudioTargets.items(config, targets)
        val first = config.items.firstOrNull { it.slot == screen.slots.first() } ?: items.firstOrNull()
        val pane = Pane(SIZE)
        if (first == null) {
            pane.back(context, player)
            pane.fillNavRow()
            return pane.menu("Item Studio · nothing here")
        }
        val isLive = items.any { it.liveGroup != null && it.pageControl == null }
        val tab = if (screen.tab == StudioTab.LIVE && !isLive) StudioTab.LOOK else screen.tab

        fun edit(transform: (InventoryItemConfiguration) -> InventoryItemConfiguration) {
            context.change(player, config.id) { StudioTargets.edit(it, targets, transform) }
            context.render(player, screen)
        }

        fun tabItem(slot: Int, which: StudioTab, material: String, label: String, enabled: Boolean = true) {
            if (!enabled) {
                pane[slot] = Ui.disabled(label, listOf("<hnt>Only for live slots."))
                return
            }
            pane.left(slot, Ui.option(material, label, emptyList(), selected = tab == which)) {
                if (tab != which) context.replace(player, screen.copy(tab = which))
            }
        }

        tabItem(LOOK_TAB, StudioTab.LOOK, "PAINTING", "Look")
        tabItem(CLICK_TAB, StudioTab.CLICK, "LEVER", "Click")
        tabItem(LIVE_TAB, StudioTab.LIVE, "LIME_STAINED_GLASS_PANE", "Live", enabled = isLive)
        val livePreview = session.live?.slots?.get(first.slot)?.takeIf { session.mode == CanvasMode.LIVE }
        pane[PREVIEW_SLOT] = livePreview
            ?.let { it.copy(lore = it.lore + Palette.expand("<hnt>Live preview from the canvas")) }
            ?: preview(first)
        pane.fill(HEADER_FILLER)

        when (tab) {
            StudioTab.LOOK -> look(context, player, config, screen, items, pane, ::edit)
            StudioTab.CLICK -> click(context, player, config, items, targets, pane)
            StudioTab.LIVE -> live(context, player, config, screen, items, pane)
        }

        if (targets.size > 1) {
            pane[COUNT_SLOT] = Ui.item(
                "CHEST_MINECART",
                "<ttl>Editing <val>${targets.size} <ttl>items",
                listOf("<bd>Changes here apply to all of them.", "<hnt>Fields that differ show as mixed."),
                amount = targets.size.coerceIn(1, 64),
            )
        }
        val deleted = if (targets.size > 1) "these ${targets.size} items" else "this item"
        pane.left(
            DELETE_SLOT,
            Ui.item(
                "TNT",
                "<err>Delete",
                listOf("<bd>Removes $deleted.", "<hnt>Undo on the canvas brings it back.", "", "<key>Left <hnt>Delete"),
            ),
        ) {
            context.change(player, config.id) { cfg ->
                cfg.copy(items = cfg.items.filterNot { it.slot in targets }.toMutableList())
            }
            context.session(player).selection.removeAll(targets)
            context.back(player)
        }
        pane.back(context, player)
        pane.fillNavRow()

        val title = first.name?.let(Msg.miniMessage::stripTags)?.takeIf { it.isNotBlank() }
            ?: Ui.pretty(first.material)

        return pane.menu(Ui.title("Item Studio", title))
    }

    private fun preview(item: InventoryItemConfiguration): NpcItem {
        val icon = ItemIconResolver.icon(item)
        return icon.copy(name = icon.name.ifBlank { Ui.pretty(item.material) }, hideTooltip = false)
    }

    private fun look(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.Studio,
        items: List<InventoryItemConfiguration>,
        pane: Pane,
        edit: ((InventoryItemConfiguration) -> InventoryItemConfiguration) -> Unit,
    ) {
        val slots = items.map { it.slot }
        val material = StudioTargets.shared(items) { it.material }
        pane.left(
            MATERIAL_SLOT,
            Ui.item(
                (material as? Shared.Same)?.value ?: "BUNDLE",
                "<ttl>Material",
                listOf(valueLine(material) { Ui.pretty(it) }, "", "<key>Left <hnt>Pick another"),
            ),
        ) { context.navigate(player, InventoryEditorScreen.Picker(config.id, InventoryPickerPurpose.Material(slots))) }

        val name = StudioTargets.shared(items) { it.name }
        pane.on(
            NAME_SLOT,
            Ui.item(
                "NAME_TAG",
                "<ttl>Name",
                listOf(
                    when (name) {
                        Shared.Mixed -> "<warn>mixed"
                        is Shared.Same -> name.value?.let { "<bd>Now: $it" } ?: "<off>Default item name"
                    },
                    "<hnt>MiniMessage and <entry_*> placeholders work.",
                    "",
                    "<key>Left <hnt>Type it in chat  <key>Q <hnt>Clear",
                ),
            ),
        ) { click ->
            when (click) {
                MenuClick.LEFT -> prompt(
                    context,
                    player,
                    screen,
                    "Item Name",
                    "Type the item name in chat.",
                    (name as? Shared.Same)?.value,
                    placeholders = PromptTokens.MENU +
                        PromptTokens.ENTRY.takeIf { items.any { it.liveGroup != null } }.orEmpty(),
                ) { text ->
                    context.change(player, config.id) {
                        StudioTargets.edit(it, slots.toSet()) { item -> item.copy(name = text) }
                    }
                }
                MenuClick.DROP -> edit { it.copy(name = null) }
                else -> Unit
            }
        }

        val lore = StudioTargets.shared(items) { it.lore.toList() }
        pane.left(
            LORE_SLOT,
            Ui.item(
                "WRITABLE_BOOK",
                "<ttl>Lore",
                when (lore) {
                    Shared.Mixed -> listOf("<warn>mixed")
                    is Shared.Same -> lore.value
                        .ifEmpty { listOf("<off>No lore") }
                        .take(6)
                        .map { "<bd>" + it.ifBlank { " " } }
                } + listOf("", "<key>Left <hnt>Edit the lines"),
            ),
        ) { context.navigate(player, InventoryEditorScreen.Lore(config.id, slots)) }

        val amount = (StudioTargets.shared(items) { it.amount } as? Shared.Same)?.value ?: 1
        pane[AMOUNT_SLOT] = AMOUNT.element(amount.toDouble()) { value ->
            edit { it.copy(amount = value.toInt()) }
        }

        val glint = (StudioTargets.shared(items) { it.glowing } as? Shared.Same)?.value
        pane.toggle(GLINT_SLOT, "Enchant Glint", glint == true, material = "EXPERIENCE_BOTTLE") {
            edit { it.copy(glowing = glint != true) }
        }

        val hidden = (StudioTargets.shared(items) { it.hideTooltip } as? Shared.Same)?.value
        pane.toggle(
            TOOLTIP_SLOT,
            "Hide Tooltip",
            hidden == true,
            extraLore = listOf("<hnt>For decoration; hovering shows nothing."),
            material = "GLASS_PANE",
        ) { edit { it.copy(hideTooltip = hidden != true) } }

        val model = (StudioTargets.shared(items) { it.customModelData } as? Shared.Same)?.value ?: 0
        pane[MODEL_SLOT] = MODEL_DATA.element(model.toDouble()) { value ->
            edit { it.copy(customModelData = value.toInt().takeIf { data -> data > 0 }) }
        }

        val head = StudioTargets.shared(items) { it.head }
        val isHead = items.all { it.material == "PLAYER_HEAD" }
        if (!isHead) {
            pane[HEAD_SLOT] = Ui.disabled(
                "Head Texture",
                listOf("<hnt>For player heads; set the material to Player Head."),
            )
        } else {
            pane.on(
                HEAD_SLOT,
                Ui.item(
                    "PLAYER_HEAD",
                    "<ttl>Head Texture",
                    listOf(
                        when (head) {
                            Shared.Mixed -> "<warn>mixed"
                            is Shared.Same -> headLine(head.value)
                        },
                        "<hnt>A texture value (e.g. from minecraft-heads.com),",
                        "<hnt>a username or UUID, or whoever opens the menu.",
                        "",
                        "<key>Left <hnt>Type it in chat  <key>Right <hnt>Viewer's head",
                        "<key>Q <hnt>Clear",
                    ),
                ),
            ) { click ->
                when (click) {
                    MenuClick.LEFT -> prompt(
                        context,
                        player,
                        screen,
                        "Player Head",
                        "Paste a texture value, or type a username or UUID.",
                        (head as? Shared.Same)?.value?.owner,
                        format = PromptFormat.PLAIN,
                        validate = {
                            if (headOf(it) != null) null
                            else PromptResult.Rejected(Component.text("That is no texture, username or UUID."))
                        },
                    ) { text ->
                        val parsed = headOf(text) ?: return@prompt
                        parsed.owner?.let(HeadOwners::skin)
                        context.change(player, config.id) {
                            StudioTargets.edit(it, slots.toSet()) { item -> item.copy(head = parsed) }
                        }
                    }
                    MenuClick.RIGHT -> {
                        val viewer = (head as? Shared.Same)?.value?.showsViewer == true
                        edit { it.copy(head = if (viewer) null else HeadConfiguration(viewer = true)) }
                    }
                    MenuClick.DROP -> edit { it.copy(head = null) }
                    else -> Unit
                }
            }
        }
    }

    private fun headLine(head: HeadConfiguration?): String = when {
        head == null -> "<off>Plain head"
        head.showsViewer -> "<on>The viewer's own head"
        head.owner != null -> "<bd>Head of <val>${head.owner}"
        else -> "<bd>Custom texture set"
    }

    private fun headOf(input: String): HeadConfiguration? = when (val parsed = SkinInput.parse(input)) {
        is SkinInput.Uuid -> HeadConfiguration(owner = parsed.uuid.toString())
        is SkinInput.Username -> HeadConfiguration(owner = parsed.name)
        is SkinInput.Texture -> HeadConfiguration(texture = parsed.value, signature = parsed.signature)
        null -> null
    }

    private fun click(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        items: List<InventoryItemConfiguration>,
        targets: Set<Int>,
        pane: Pane,
    ) {
        val slots = targets.sorted()
        val first = items.first()
        CLICK_SLOTS.forEach { (interaction, slot) ->
            val action = ItemClickActions.of(first, interaction)
            val configured = action?.configuredTypes().orEmpty()
            pane.left(
                slot,
                Ui.item(
                    Clicks.material(interaction),
                    "<ttl>${Clicks.label(interaction)}",
                    configured.map { "<bd>· <val>$it" }.ifEmpty { listOf("<off>Nothing") } +
                        listOf("", "<key>Left <hnt>Edit"),
                    glowing = configured.isNotEmpty(),
                ),
            ) { context.navigate(player, InventoryEditorScreen.ItemAction(config.id, slots, interaction)) }
        }

        val legacy = first.actions.isNotEmpty() &&
            first.actions.all { it.interactionType == PlayerInteraction.RIGHT_CLICK }
        val liveSource = first.liveGroup?.let { LiveGroups.source(config, it) }
        pane[CLICK_INFO_SLOT] = Ui.item(
            "BOOK",
            "<ttl>How Clicks Work",
            listOfNotNull(
                "<bd>Each click type runs its own actions;",
                "<bd>shift clicks fall back to the plain one.",
                "<warn>Only right click is set, so it runs on any click.".takeIf { legacy },
                "<hnt>Live slot with no actions: the source's default runs.".takeIf { liveSource != null },
            ),
        )
    }

    private fun live(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.Studio,
        items: List<InventoryItemConfiguration>,
        pane: Pane,
    ) {
        val session = context.session(player)
        val group = items.firstNotNullOfOrNull { it.liveGroup } ?: return
        val (color, _) = LiveGroups.colorOf(config, group)
        val sourceLabel = LiveGroups.source(config, group)?.let { LiveGroups.describe(it.type).label } ?: "none"
        pane.left(
            GROUP_SLOT,
            Ui.item(
                "${color}_STAINED_GLASS_PANE",
                "<ttl>Group <val>$group",
                listOf(
                    "<bd>Source <val>$sourceLabel",
                    "<bd><val>${LiveGroups.contentSlots(config, group).size} <bd>slots",
                    "",
                    "<key>Left <hnt>Group settings",
                ),
            ),
        ) { context.navigate(player, InventoryEditorScreen.LiveGroup(config.id, group)) }

        pane.toggle(
            SCOPE_SLOT,
            "This Slot Only",
            session.thisSlotOnly,
            extraLore = listOf("<hnt>Off: Look edits restyle every slot", "<hnt>of the group at once."),
            material = "TARGET",
        ) {
            session.thisSlotOnly = !session.thisSlotOnly
            context.render(player, screen)
        }

        pane[PLACEHOLDERS_SLOT] = Ui.item(
            "OAK_SIGN",
            "<ttl>Placeholders",
            PromptTokens.ENTRY.map { "<val>${it.token} <hnt>${it.description}" },
        )
    }

    private fun valueLine(shared: Shared<String>, format: (String) -> String): String = when (shared) {
        Shared.Mixed -> "<warn>mixed"
        is Shared.Same -> "<bd>Now: <val>${format(shared.value)}"
    }

    private fun prompt(
        context: InventoryEditorContext,
        player: NpcPlayer,
        screen: InventoryEditorScreen.Studio,
        title: String,
        instruction: String,
        current: String?,
        format: PromptFormat = PromptFormat.MINI_MESSAGE,
        placeholders: List<PromptPlaceholder> = emptyList(),
        validate: (String) -> PromptResult.Rejected? = PromptResult::rejectInvalidMiniMessage,
        apply: (String) -> Unit,
    ) {
        context.chatPrompt(
            player,
            Prompt(
                title = title,
                instruction = instruction,
                current = current,
                format = format,
                placeholders = placeholders,
                onCancel = { context.render(player, screen) },
                onSubmit = { input ->
                    val text = input.trim()
                    validate(text)?.let { return@Prompt it }
                    apply(text)
                    context.render(player, screen)
                    PromptResult.Accepted
                },
            ),
        )
    }

}
