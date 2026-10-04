package app.simplecloud.npc.common.editor.inventory.screens

import app.simplecloud.npc.common.editor.core.PickerOption
import app.simplecloud.npc.common.editor.core.PickerOptions
import app.simplecloud.npc.common.editor.core.PickerResult
import app.simplecloud.npc.common.editor.core.PickerSpec
import app.simplecloud.npc.common.editor.core.PickerTemplate
import app.simplecloud.npc.common.editor.core.SoundTuning
import app.simplecloud.npc.common.editor.core.pageKey
import app.simplecloud.npc.common.editor.inventory.Carried
import app.simplecloud.npc.common.editor.inventory.CommonMaterials
import app.simplecloud.npc.common.editor.inventory.InventoryEditorContext
import app.simplecloud.npc.common.editor.inventory.InventoryEditorScreen
import app.simplecloud.npc.common.editor.inventory.InventoryEditorSession
import app.simplecloud.npc.common.editor.inventory.InventoryPickerPurpose
import app.simplecloud.npc.common.editor.inventory.LiveGroups
import app.simplecloud.npc.common.editor.inventory.StudioTargets
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.Pane
import app.simplecloud.npc.common.editor.ui.Ui
import app.simplecloud.npc.common.inventory.ItemClickActions
import app.simplecloud.npc.common.inventory.source.GroupServersSource
import app.simplecloud.npc.core.config.NpcConfig.ActionConfiguration
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer

object InventoryPickers {

    private val CATEGORY_SLOTS = listOf(27, 28, 29, 30, 32, 33, 34, 35, 36)

    fun build(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        purpose: InventoryPickerPurpose,
    ): EditorMenu {
        val screen = InventoryEditorScreen.Picker(config.id, purpose)
        val spec = when (purpose) {
            InventoryPickerPurpose.NewItem,
            is InventoryPickerPurpose.Material,
            is InventoryPickerPurpose.LookMaterial,
            -> materials(context, player, config, screen, purpose)

            InventoryPickerPurpose.NewLiveGroup -> sourceTypes(context, player, config, screen, null)
            is InventoryPickerPurpose.LiveSourceType -> sourceTypes(context, player, config, screen, purpose.group)
            is InventoryPickerPurpose.LiveSourceGroup -> sourceGroups(context, player, config, screen, purpose.group)
            InventoryPickerPurpose.UsedBy -> usedBy(context, player, config, screen)
            is InventoryPickerPurpose.OpenInventory -> openInventory(context, player, config, screen, purpose)
            is InventoryPickerPurpose.SendToServer -> sendToServer(context, player, config, screen, purpose)
            is InventoryPickerPurpose.Sound -> sounds(context, player, config, screen, purpose)
        }

        return PickerTemplate.build(context, player, spec)
    }

    private fun materials(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.Picker,
        purpose: InventoryPickerPurpose,
    ): PickerSpec<InventoryEditorScreen> {
        val session = context.session(player)
        val catalog = context.materialCatalog()
        val known = catalog.toSet()
        val filter = (session.pickerResult as? PickerResult.Filtered)?.typed
        val current = when (purpose) {
            is InventoryPickerPurpose.Material -> purpose.slots.firstOrNull()?.let { slot ->
                config.items.firstOrNull { it.slot == slot }?.material
            }

            is InventoryPickerPurpose.LookMaterial ->
                LiveGroups.source(config, purpose.group)?.lookFor(purpose.state)?.material

            else -> null
        }

        val listed = when {
            filter != null -> catalog.filter { it.contains(filter, ignoreCase = true) }
            session.materialCategory == InventoryEditorSession.RECENT_CATEGORY ->
                session.recentMaterials.ifEmpty { CommonMaterials.COMMON.take(RECENT_FALLBACK) }

            session.materialCategory == InventoryEditorSession.ALL_CATEGORY -> catalog.sorted()
            else -> CommonMaterials.CATEGORIES.firstOrNull { it.first == session.materialCategory }?.second.orEmpty()
        }.filter { it in known }

        fun pick(material: String) {
            val canonical = catalog.firstOrNull { it.equals(material, true) } ?: return
            session.usedMaterial(canonical)
            session.pickerResult = null
            when (purpose) {
                is InventoryPickerPurpose.Material -> context.change(player, config.id) { fresh ->
                    StudioTargets.edit(fresh, purpose.slots.toSet()) { it.copy(material = canonical) }
                }

                is InventoryPickerPurpose.LookMaterial -> context.change(player, config.id) { fresh ->
                    LiveGroups.setLook(fresh, purpose.group, purpose.state) { it.copy(material = canonical) }
                }

                else -> session.carried = Carried(InventoryItemConfiguration(material = canonical), from = null)
            }
            context.back(player)
        }

        val section = if (purpose is InventoryPickerPurpose.NewItem) "New Item" else "Material"

        return PickerSpec(
            title = "$section · ${config.id}",
            screen = screen,
            options = listed.map { material ->
                val selected = material == current
                PickerOption(
                    material,
                    Ui.option(material, Ui.pretty(material), listOf("<hnt>$material", ""), selected = selected),
                    available = !selected,
                )
            },
            onPick = ::pick,
            typeHint = PickerOptions.MATERIAL_TYPE_HINT,
            onTyped = { typed ->
                PickerOptions.search(catalog, typed, "material") ?: run {
                    pick(typed)
                    null
                }
            },
            extras = { pane -> categoryChips(context, player, screen, pane) },
            filteredNoun = "materials",
        )
    }

    private fun categoryChips(
        context: InventoryEditorContext,
        player: NpcPlayer,
        screen: InventoryEditorScreen.Picker,
        pane: Pane,
    ) {
        val session = context.session(player)
        val filtered = session.pickerResult is PickerResult.Filtered
        val names = listOf(InventoryEditorSession.RECENT_CATEGORY) + CommonMaterials.CATEGORIES.map { it.first } +
            InventoryEditorSession.ALL_CATEGORY

        names.zip(CATEGORY_SLOTS).forEach { (name, slot) ->
            val selected = !filtered && session.materialCategory == name
            pane.left(slot, Ui.option("NAME_TAG", name, emptyList(), selected)) {
                session.materialCategory = name
                session.pickerResult = null
                session.pages[pageKey(screen)] = 0
                context.render(player, screen)
            }
        }
    }

    private fun sourceTypes(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.Picker,
        group: String?,
    ): PickerSpec<InventoryEditorScreen> {
        val current = group?.let { LiveGroups.source(config, it)?.type }
        return PickerSpec(
            title = if (group == null) "New Live Group · ${config.id}" else "Source · $group",
            screen = screen,
            options = context.dataSourceTypes().map { type ->
                val info = LiveGroups.describe(type)
                PickerOption(
                    type,
                    Ui.option(info.icon, info.label, info.lines.map { "<bd>$it" } + "", selected = type == current),
                    available = type != current,
                )
            },
            typeHint = null,
            typingUnavailable = "Sources come from the plugin and its addons.",
            onPick = { type ->
                if (group == null) {
                    val name = LiveGroups.freeName(config, type)
                    val session = context.session(player)
                    session.carried = Carried(LiveGroups.newSlot(name, type), from = null)
                    session.activeGroup = name
                } else {
                    context.change(player, config.id) {
                        LiveGroups.setSource(it, group) { source -> source.copy(type = type) }
                    }
                }
                context.back(player)
            },
        )
    }

    private fun sourceGroups(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.Picker,
        group: String,
    ): PickerSpec<InventoryEditorScreen> {
        val current = LiveGroups.source(config, group)?.group ?: GroupServersSource.TARGET_GROUP
        val targetOption = PickerOption(
            GroupServersSource.TARGET_GROUP,
            Ui.option(
                "ARMOR_STAND",
                "The NPC's Target",
                listOf("<bd>Whatever group the NPC that", "<bd>opened the menu targets.", ""),
                selected = current == GroupServersSource.TARGET_GROUP,
            ),
            available = current != GroupServersSource.TARGET_GROUP,
        )
        val groups = context.liveData.knownTargets().filter { it.group }.map { target ->
            PickerOption(
                target.name,
                Ui.option(
                    "PAPER",
                    target.name,
                    listOf("<bd>Live <val>${context.liveData.summaryOf(target.name).liveServers} <bd>servers", ""),
                    selected = target.name.equals(current, true),
                ),
                available = !target.name.equals(current, true),
            )
        }

        return PickerSpec(
            title = "Group · $group",
            screen = screen,
            options = listOf(targetOption) + groups,
            onPick = { value ->
                context.change(player, config.id) {
                    LiveGroups.setSource(it, group) { source -> source.copy(group = value) }
                }
                context.back(player)
            },
        )
    }

    private fun usedBy(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.Picker,
    ): PickerSpec<InventoryEditorScreen> = PickerSpec(
        title = "Used By · ${config.id}",
        screen = screen,
        options = InventoryHubMenuBuilder.usedBy(context, config.id).map { npc ->
            PickerOption(
                npc.id,
                Ui.item("ARMOR_STAND", "<ttl>${npc.id}", listOf("", "<key>Left <info>Open its editor")),
            )
        },
        typeHint = null,
        typingUnavailable = "Only NPCs that open this menu are listed.",
        onPick = { id ->
            val npc = context.npcRepository.find(id) ?: return@PickerSpec
            context.close(player)
            context.editors.openNpc(player, npc)
        },
    )

    private fun openInventory(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.Picker,
        purpose: InventoryPickerPurpose.OpenInventory,
    ): PickerSpec<InventoryEditorScreen> = PickerSpec(
        title = "Open Menu · ${config.id}",
        screen = screen,
        options = PickerOptions.menus(
            context.inventoryRepository.findAll(),
            currentAction(config, purpose.slots, purpose.interaction)?.openInventory,
        ),
        typeHint = null,
        typingUnavailable = "Create the menu first, then pick it here.",
        onPick = { id ->
            editClicks(context, player, config, purpose.slots, purpose.interaction) { it.openInventory = id }
            context.back(player)
        },
    )

    private fun sendToServer(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.Picker,
        purpose: InventoryPickerPurpose.SendToServer,
    ): PickerSpec<InventoryEditorScreen> {
        val names = context.liveData.servers().map { it.name }

        fun apply(server: String) {
            val canonical = PickerOptions.canonical(names, server)
            editClicks(context, player, config, purpose.slots, purpose.interaction) { it.sendToServer = canonical }
            context.back(player)
        }

        return PickerSpec(
            title = "Send To Server · ${config.id}",
            screen = screen,
            options = PickerOptions.servers(
                context.liveData.servers(),
                currentAction(config, purpose.slots, purpose.interaction)?.sendToServer,
            ),
            onPick = ::apply,
            onTyped = { typed ->
                PickerOptions.match(names, typed, "No server by that name.").also { result ->
                    if (result == null) apply(typed)
                }
            },
        )
    }

    private fun sounds(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        screen: InventoryEditorScreen.Picker,
        purpose: InventoryPickerPurpose.Sound,
    ): PickerSpec<InventoryEditorScreen> {
        val session = context.session(player)
        val catalog = context.catalogs.sounds()
        val filter = (session.pickerResult as? PickerResult.Filtered)?.typed
        val action = currentAction(config, purpose.slots, purpose.interaction)
        val options = action?.playSoundOptions ?: SoundTuning.DEFAULTS

        fun pick(sound: String) {
            val canonical = PickerOptions.canonical(catalog, sound)
            val filtered = session.pickerResult is PickerResult.Filtered
            editClicks(context, player, config, purpose.slots, purpose.interaction) { it.playSound = canonical }
            if (filtered) {
                context.render(player, screen)
            } else {
                val result = PickerResult.Added(canonical, "Tune volume and pitch below, or go Back.")
                PickerTemplate.showResult(context, player, screen, result)
            }
        }

        return PickerSpec(
            title = "Pick Sound · ${config.id}",
            screen = screen,
            options = PickerOptions.sounds(catalog, filter, action?.playSound),
            onPick = ::pick,
            typeHint = PickerOptions.SOUND_TYPE_HINT,
            onTyped = { typed ->
                PickerOptions.search(catalog, typed, "sound key") ?: run {
                    pick(typed)
                    null
                }
            },
            onRightClick = { option -> player.playSound(option.value, options) },
            extras = { pane ->
                SoundTuning.place(pane, context.textPrompts, player, action?.playSound, options) { updated ->
                    val changed = context.change(player, config.id) { fresh ->
                        val items = fresh.items.filter { it.slot in purpose.slots }
                        val actions = items.mapNotNull { ItemClickActions.of(it, purpose.interaction) }
                        if (actions.none { it.playSound != null }) return@change null
                        actions.forEach { it.playSoundOptions = updated(it.playSoundOptions) }
                        fresh
                    }
                    if (changed) context.render(player, screen)
                }
            },
            filteredNoun = "sounds",
        )
    }

    private fun currentAction(
        config: InventoryConfiguration,
        slots: List<Int>,
        interaction: PlayerInteraction,
    ): ActionConfiguration? =
        config.items.firstOrNull { it.slot in slots }?.let { ItemClickActions.of(it, interaction) }

    private fun editClicks(
        context: InventoryEditorContext,
        player: NpcPlayer,
        config: InventoryConfiguration,
        slots: List<Int>,
        interaction: PlayerInteraction,
        edit: (ActionConfiguration) -> Unit,
    ) {
        context.change(player, config.id) { fresh ->
            fresh.items.filter { it.slot in slots }.forEach { edit(ItemClickActions.orCreate(it, interaction)) }
            fresh
        }
    }

    private const val RECENT_FALLBACK = 27
}
