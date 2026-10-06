package app.simplecloud.npc.common.editor.inventory.screens

import app.simplecloud.api.server.ServerState
import app.simplecloud.npc.common.editor.Prompt
import app.simplecloud.npc.common.editor.PromptFormat
import app.simplecloud.npc.common.editor.PromptResult
import app.simplecloud.npc.common.editor.PromptTokens
import app.simplecloud.npc.common.editor.core.back
import app.simplecloud.npc.common.editor.core.toggle
import app.simplecloud.npc.common.editor.inventory.InventoryConfirmPurpose
import app.simplecloud.npc.common.editor.inventory.InventoryEditorContext
import app.simplecloud.npc.common.editor.inventory.InventoryEditorScreen
import app.simplecloud.npc.common.editor.inventory.InventoryPickerPurpose
import app.simplecloud.npc.common.editor.inventory.LiveGroups
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.editor.ui.after
import app.simplecloud.npc.common.inventory.source.GroupServersSource
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer
import net.kyori.adventure.text.Component

object LiveGroupMenuBuilder {
    private const val SIZE = 45

    private const val HEADER_SLOT = 4
    private const val SOURCE_SLOT = 10
    private const val GROUP_SLOT = 12
    private const val SORT_SLOT = 14
    private const val EMPTY_SLOT = 16
    private val STATE_SLOTS = 19..25
    private const val RENAME_SLOT = 30
    private const val DELETE_SLOT = 32
    private const val LOOKS_SLOT = 34

    private val SORTS = listOf("NUMERICAL_ID", "PLAYER_COUNT")
    private val EMPTY_LOOK = InventoryItemConfiguration(
        material = "GRAY_STAINED_GLASS_PANE",
        name = "<#475569>Spinning up...",
    )

    fun build(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        group: String,
    ): EditorMenu {
        val screen = InventoryEditorScreen.LiveGroup(config.id, group)
        val source = LiveGroups.source(config, group)
        val type = source?.type
        val (color, _) = LiveGroups.colorOf(config, group)
        val pane = Pane(SIZE)

        fun change(mutate: (InventoryConfiguration) -> InventoryConfiguration?) {
            context.change(player, config.id, mutate)
            context.render(player, screen)
        }

        fun pick(purpose: InventoryPickerPurpose) =
            context.navigate(player, InventoryEditorScreen.Picker(config.id, purpose))

        if (LiveGroups.members(config, group).isEmpty()) {
            pane.back(context, player)
            pane.fillNavRow()
            return pane.menu(Ui.title("Live Group", group))
        }

        pane[HEADER_SLOT] = Ui.item(
            "${color}_STAINED_GLASS_PANE",
            "<ttl>Live Group <val>$group",
            listOfNotNull(
                "<bd><val>${LiveGroups.contentSlots(config, group).size} <bd>slots",
                if (LiveGroups.lacksPageControls(config, group)) {
                    "<warn>No page arrows: extra entries are hidden."
                } else {
                    "<bd>Has page arrows"
                },
            ),
        )

        val info = type?.let(LiveGroups::describe)
        pane.left(
            SOURCE_SLOT,
            Ui.item(
                info?.icon ?: "BARRIER",
                "<ttl>Source",
                listOf("<bd>Now: <val>${info?.label ?: "none"}", "", "<key>Left <hnt>Change"),
            ),
        ) { pick(InventoryPickerPurpose.LiveSourceType(group)) }

        if (type == GroupServersSource.TYPE) {
            val target = source.group?.takeIf { it.isNotBlank() } ?: GroupServersSource.TARGET_GROUP
            val targetLabel = if (target == GroupServersSource.TARGET_GROUP) "the opening NPC's target" else target
            pane.left(
                GROUP_SLOT,
                Ui.item("PAPER", "<ttl>Which Group", listOf("<bd>Now: <val>$targetLabel", "", "<key>Left <hnt>Change")),
            ) { pick(InventoryPickerPurpose.LiveSourceGroup(group)) }

            val sort = source.sort?.uppercase() ?: SORTS.first()
            val sortLabel = if (sort == "PLAYER_COUNT") "most players first" else "by number"
            pane.left(
                SORT_SLOT,
                Ui.item("HOPPER", "<ttl>Order", listOf("<bd>Now: <val>$sortLabel", "", "<key>Left <hnt>Switch")),
            ) {
                val next = SORTS.after(sort)
                change { LiveGroups.setSource(it, group) { s -> s.copy(sort = next) } }
            }

            val chosen = source.states.map { it.uppercase() }.toSet()
            val noneHint = listOfNotNull("<hnt>None picked: everything but stopping.".takeIf { chosen.isEmpty() })
            val states = ServerState.entries.filterNot { it.name.startsWith("UNKNOWN") }
            states.zip(STATE_SLOTS.toList()).forEach { (state, slot) ->
                val on = state.name in chosen
                pane.toggle(
                    slot,
                    state.name.lowercase().replace('_', ' ').replaceFirstChar(Char::uppercase),
                    on,
                    extraLore = noneHint,
                ) {
                    change {
                        LiveGroups.setSource(it, group) { s ->
                            val states = s.states.map(String::uppercase).toMutableSet()
                            if (on) states -= state.name else states += state.name
                            s.copy(states = states.sorted().toMutableList())
                        }
                    }
                }
            }
        }

        val emptyLook = LiveGroups.whenEmpty(config, group)
        pane.on(
            EMPTY_SLOT,
            Ui.item(
                emptyLook?.material ?: "LIGHT_GRAY_STAINED_GLASS_PANE",
                "<ttl>When A Slot Is Empty",
                listOf(
                    emptyLook?.let { "<bd>Shows " + (it.name ?: it.material) } ?: "<off>Stays empty",
                    "",
                    "<key>Left <hnt>${if (emptyLook == null) "Show a placeholder" else "Leave empty slots empty"}",
                    "<key>Right <hnt>Type the placeholder's name",
                ),
            ),
        ) { click ->
            when (click) {
                MenuClick.LEFT -> change {
                    LiveGroups.setWhenEmpty(it, group, if (emptyLook == null) EMPTY_LOOK else null)
                }
                MenuClick.RIGHT -> context.chatPrompt(
                    player,
                    Prompt(
                        title = "Empty Slot Name",
                        instruction = "Type what an empty slot is called in chat.",
                        current = emptyLook?.name,
                        format = PromptFormat.MINI_MESSAGE,
                        placeholders = PromptTokens.MENU,
                        onCancel = { context.render(player, screen) },
                        onSubmit = { input ->
                            val name = input.trim()
                            PromptResult.rejectInvalidMiniMessage(name)?.let { return@Prompt it }
                            change { LiveGroups.setWhenEmpty(it, group, (emptyLook ?: EMPTY_LOOK).copy(name = name)) }
                            PromptResult.Accepted
                        },
                    ),
                )
                else -> Unit
            }
        }

        pane.left(
            RENAME_SLOT,
            Ui.item(
                "ANVIL",
                "<ttl>Rename",
                listOf("<bd>Now: <val>$group", "", "<key>Left <hnt>Type a new name in chat"),
            ),
        ) {
            context.chatPrompt(
                player,
                Prompt(
                    title = "Rename Live Group",
                    instruction = "Type the new name of the live group in chat.",
                    current = group,
                    onCancel = { context.render(player, screen) },
                    onSubmit = { input ->
                        val name = input.trim()
                        if (name.isBlank() || name in LiveGroups.names(config)) {
                            return@Prompt PromptResult.Rejected(Component.text("Pick a name no other group uses."))
                        }
                        val renamed = context.change(player, config.id) { LiveGroups.rename(it, group, name) }
                        if (!renamed) {
                            return@Prompt PromptResult.Rejected(Component.text("Pick a name no other group uses."))
                        }
                        context.session(player).renameGroup(group, name)
                        context.replace(player, InventoryEditorScreen.LiveGroup(config.id, name))
                        PromptResult.Accepted
                    },
                ),
            )
        }

        val looks = source?.looks?.size ?: 0
        if (StateLooksMenuBuilder.statesFor(type).isNotEmpty()) {
            pane.left(
                LOOKS_SLOT,
                Ui.item(
                    "PAINTING",
                    "<ttl>State Looks",
                    listOf(
                        if (looks == 0) "<off>None set" else "<bd><val>$looks <bd>states styled",
                        "<hnt>Full, starting, offline… each its own look.",
                        "",
                        "<key>Left <hnt>Open",
                    ),
                    glowing = looks > 0,
                ),
            ) { context.navigate(player, InventoryEditorScreen.StateLooks(config.id, group)) }
        }

        pane.left(
            DELETE_SLOT,
            Ui.item(
                "TNT",
                "<err>Delete Group",
                listOf("<bd>Removes all its slots and arrows.", "", "<key>Left <hnt>Asks first"),
            ),
        ) {
            val purpose = InventoryConfirmPurpose.DeleteLiveGroup(group)
            context.navigate(player, InventoryEditorScreen.Confirm(config.id, purpose))
        }

        pane.back(context, player)
        pane.fillNavRow()

        return pane.menu(Ui.title("Live Group", group))
    }
}
