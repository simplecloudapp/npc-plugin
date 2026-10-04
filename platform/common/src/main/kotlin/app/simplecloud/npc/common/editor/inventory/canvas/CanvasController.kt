package app.simplecloud.npc.common.editor.inventory.canvas

import app.simplecloud.npc.common.editor.canvas.CanvasInput
import app.simplecloud.npc.common.editor.canvas.CanvasSurface
import app.simplecloud.npc.common.editor.inventory.CanvasMode
import app.simplecloud.npc.common.editor.inventory.CanvasOps
import app.simplecloud.npc.common.editor.inventory.Carried
import app.simplecloud.npc.common.editor.inventory.InventoryEditorContext
import app.simplecloud.npc.common.editor.inventory.InventoryEditorScreen
import app.simplecloud.npc.common.editor.inventory.InventoryEditorSession
import app.simplecloud.npc.common.editor.inventory.InventoryPickerPurpose
import app.simplecloud.npc.common.editor.inventory.LiveGroups
import app.simplecloud.npc.common.editor.inventory.screens.InventoryHubMenuBuilder
import app.simplecloud.npc.common.inventory.source.InventoryOpenContext
import app.simplecloud.npc.common.text.sendInfo
import app.simplecloud.npc.common.utils.BackgroundTasks
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.PageControl
import app.simplecloud.npc.core.platform.NpcPlayer
import kotlinx.coroutines.CancellationException

class CanvasController(
    private val context: InventoryEditorContext,
    private val surface: CanvasSurface,
    private val onEscape: (NpcPlayer) -> Unit,
) {

    fun show(player: NpcPlayer, screen: InventoryEditorScreen.Canvas) {
        val config = context.inventoryRepository.find(screen.inventoryId) ?: return
        val session = context.session(player)
        if (session.mode == CanvasMode.LIVE && session.live == null) requestLive(player, screen)

        session.lastCanvas = config
        surface.open(
            player,
            CanvasRender.view(config, session),
            onInput = { input -> handle(player, screen, input) },
            onClose = { onEscape(player) },
        )
    }

    fun refresh(player: NpcPlayer) {
        val top = context.sessions.peek(player.uniqueId)?.stack?.lastOrNull()
        val screen = top as? InventoryEditorScreen.Canvas ?: return
        if (context.chatInputPrompts.isPending(player.uniqueId)) return
        val session = context.session(player)

        when {
            session.mode == CanvasMode.LIVE -> requestLive(player, screen)
            context.inventoryRepository.find(screen.inventoryId) != session.lastCanvas -> context.render(player, screen)
        }
    }

    private fun handle(player: NpcPlayer, screen: InventoryEditorScreen.Canvas, input: CanvasInput) {
        val config = context.inventoryRepository.find(screen.inventoryId) ?: return context.close(player)
        val session = context.session(player)
        val size = config.size()

        when {
            session.handMode -> handMode(player, session, size, input)
            input is CanvasInput.Click && input.slot >= size ->
                toolbox(player, screen, config, session, input.slot - size, input)

            session.mode == CanvasMode.LIVE -> livePaging(player, screen, config, session, input)
            else -> canvas(player, screen, config, session, input)
        }

        if (context.isShowing(player) { it == screen }) context.render(player, screen)
    }

    private fun canvas(
        player: NpcPlayer,
        screen: InventoryEditorScreen.Canvas,
        config: InventoryConfiguration,
        session: InventoryEditorSession,
        input: CanvasInput,
    ) {
        val size = config.size()
        when (input) {
            is CanvasInput.Click -> when {
                input.shift && !input.right -> {
                    val add = input.slot !in session.selectionFor(config.id)
                    session.select(config.id, listOf(input.slot), add = add)
                }

                input.shift -> {
                    session.clearSelection()
                    if (CanvasOps.itemAt(config, input.slot) != null) {
                        session.select(config.id, CanvasOps.similar(config, input.slot))
                    }
                }

                !input.right -> leftClick(player, screen, config, session, input.slot)
                else -> rightClick(player, screen, config, session, input.slot)
            }

            is CanvasInput.Drag -> session.carried?.let { carried ->
                val targets = input.slots.filter { it < size && it != carried.from }
                if (change(player, config) { CanvasOps.stamp(it, targets, carried.item) }) placed(session, carried.item)
            }

            is CanvasInput.Key -> if (input.slot < size) key(player, screen, config, session, input.slot, input.key)
            is CanvasInput.Drop -> when {
                input.slot >= size -> Unit
                session.carried != null -> throwAway(player, config, session)
                else -> erase(player, config, session, input.slot)
            }

            is CanvasInput.Outside -> throwAway(player, config, session)
            is CanvasInput.DoubleClick, is CanvasInput.Clone -> Unit
        }
    }

    private fun leftClick(
        player: NpcPlayer,
        screen: InventoryEditorScreen.Canvas,
        config: InventoryConfiguration,
        session: InventoryEditorSession,
        slot: Int,
    ) {
        val carried = session.carried
        val existing = CanvasOps.itemAt(config, slot)
        when {
            carried != null -> drop(player, config, session, carried, slot)
            existing != null -> session.carried = Carried(existing, from = slot)
            else -> {
                val picker = InventoryEditorScreen.Picker(screen.inventoryId, InventoryPickerPurpose.NewItem)
                context.navigate(player, picker)
            }
        }
    }

    private fun rightClick(
        player: NpcPlayer,
        screen: InventoryEditorScreen.Canvas,
        config: InventoryConfiguration,
        session: InventoryEditorSession,
        slot: Int,
    ) {
        val carried = session.carried
        when {
            carried != null -> {
                val stamped = slot != carried.from && change(player, config) { CanvasOps.put(it, slot, carried.item) }
                if (stamped) placed(session, carried.item)
            }

            CanvasOps.itemAt(config, slot) != null -> openStudio(player, screen, session, config, slot)
            else -> session.lastPlaced?.let { last -> change(player, config) { CanvasOps.put(it, slot, last) } }
        }
    }

    private fun drop(
        player: NpcPlayer,
        config: InventoryConfiguration,
        session: InventoryEditorSession,
        carried: Carried,
        slot: Int,
    ) {
        val from = carried.from?.takeIf { stillAt(config, it, carried.item) }
        when {
            from == slot -> session.carried = null
            from != null -> {
                change(player, config) { CanvasOps.move(it, from, slot) }
                session.carried = null
                session.selection.remove(from)
            }

            else -> {
                val replaced = CanvasOps.itemAt(config, slot)
                if (change(player, config) { CanvasOps.put(it, slot, carried.item) }) {
                    session.carried = replaced?.let { Carried(it, from = null) }
                    placed(session, carried.item)
                }
            }
        }
    }

    private fun placed(session: InventoryEditorSession, item: InventoryItemConfiguration) {
        session.lastPlaced = item
        item.liveGroup?.let { session.activeGroup = it }
    }

    private fun key(
        player: NpcPlayer,
        screen: InventoryEditorScreen.Canvas,
        config: InventoryConfiguration,
        session: InventoryEditorSession,
        slot: Int,
        key: Int,
    ) {
        val existing = CanvasOps.itemAt(config, slot)
        when (key) {
            CanvasInput.OFFHAND_KEY, Toolbox.KEY_COPY -> when {
                key == CanvasInput.OFFHAND_KEY && session.carried != null -> session.carried = null
                existing != null -> session.carried = Carried(existing, from = null)
            }

            Toolbox.KEY_STAMP -> session.lastPlaced?.let { last ->
                change(player, config) { CanvasOps.stamp(it, targets(config, session, slot), last) }
            }

            Toolbox.KEY_ERASE -> erase(player, config, session, slot)
            Toolbox.KEY_LIVE -> {
                val group = session.activeGroup?.let { template(config, it) }
                if (group == null) player.sendInfo("Pick a live group first: left click its swatch in the toolbox.")
                else change(player, config) { CanvasOps.stamp(it, targets(config, session, slot), group) }
            }

            Toolbox.KEY_DECOR -> change(player, config) { CanvasOps.stamp(it, targets(config, session, slot), DECOR) }
            Toolbox.KEY_STUDIO -> if (existing != null) openStudio(player, screen, session, config, slot)
            in Toolbox.KEY_BINDINGS -> {
                val carried = session.carried
                val bound = session.keyBindings[key]
                when {
                    carried != null -> {
                        session.keyBindings[key] = carried.item.copy(slot = -1)
                        player.sendInfo("Bound it to key {}.", key + 1)
                    }

                    bound != null -> change(player, config) {
                        CanvasOps.stamp(it, targets(config, session, slot), bound)
                    }
                }
            }
        }
    }

    private fun erase(player: NpcPlayer, config: InventoryConfiguration, session: InventoryEditorSession, slot: Int) {
        val targets = targets(config, session, slot)
        if (change(player, config) { CanvasOps.remove(it, targets) }) session.selection.removeAll(targets.toSet())
    }

    private fun throwAway(player: NpcPlayer, config: InventoryConfiguration, session: InventoryEditorSession) {
        val carried = session.carried ?: return
        val from = carried.from
        if (from != null && stillAt(config, from, carried.item)) {
            change(player, config) { CanvasOps.remove(it, listOf(from)) }
            session.selection.remove(from)
        }
        session.carried = null
    }

    private fun toolbox(
        player: NpcPlayer,
        screen: InventoryEditorScreen.Canvas,
        config: InventoryConfiguration,
        session: InventoryEditorSession,
        index: Int,
        click: CanvasInput.Click,
    ) {
        when (index) {
            Toolbox.HUB -> context.back(player)

            Toolbox.UNDO, Toolbox.REDO -> {
                val moved = when (index) {
                    Toolbox.UNDO -> context.undo(player, config.id)
                    else -> context.redo(player, config.id)
                }
                if (moved && session.carried?.from != null) session.carried = null
                if (session.mode == CanvasMode.LIVE) requestLive(player, screen)
            }

            Toolbox.MODE -> {
                session.carried = null
                session.mode = if (session.mode == CanvasMode.LIVE) CanvasMode.DESIGN else CanvasMode.LIVE
                session.live = null
                if (session.mode == CanvasMode.LIVE) requestLive(player, screen)
            }

            Toolbox.TITLE -> context.promptTitle(player, screen.inventoryId, screen)
            Toolbox.ROWS -> rows(player, config, session, click)
            Toolbox.FILL -> {
                val empty = (0 until config.size()).filter { CanvasOps.itemAt(config, it) == null }
                change(player, config) { CanvasOps.stamp(it, empty, session.carried?.item ?: DECOR) }
            }

            Toolbox.BORDER -> {
                val size = config.size()
                val edge = (0 until size).filter { slot ->
                    val onEdge = slot < 9 || slot >= size - 9 || slot % 9 == 0 || slot % 9 == 8
                    onEdge && CanvasOps.itemAt(config, slot) == null
                }
                change(player, config) { CanvasOps.stamp(it, edge, session.carried?.item ?: DECOR) }
            }

            in Toolbox.RECENT -> {
                session.recentMaterials.getOrNull(index - Toolbox.RECENT.first)?.let { material ->
                    session.carried = Carried(InventoryItemConfiguration(material = material), from = null)
                }
            }

            Toolbox.PICK -> {
                val picker = InventoryEditorScreen.Picker(config.id, InventoryPickerPurpose.NewItem)
                context.navigate(player, picker)
            }

            Toolbox.HAND -> {
                session.carried = null
                session.handMode = true
            }

            in Toolbox.SWATCHES -> LiveGroups.names(config).getOrNull(index - Toolbox.SWATCHES.first)?.let { group ->
                session.activeGroup = group
                if (click.right) {
                    context.navigate(player, InventoryEditorScreen.LiveGroup(config.id, group))
                } else {
                    template(config, group)?.let { session.carried = Carried(it, from = null) }
                }
            }

            Toolbox.NEW_GROUP -> {
                val picker = InventoryEditorScreen.Picker(config.id, InventoryPickerPurpose.NewLiveGroup)
                context.navigate(player, picker)
            }

            Toolbox.PREVIOUS_ARROW, Toolbox.NEXT_ARROW -> {
                val group = session.activeGroup?.takeIf { it in LiveGroups.names(config) }
                if (group == null) {
                    player.sendInfo("Pick a live group first: left click its swatch in the toolbox.")
                } else {
                    val previous = index == Toolbox.PREVIOUS_ARROW
                    session.carried = Carried(
                        InventoryItemConfiguration(
                            material = "ARROW",
                            name = if (previous) "<#e2e8f0>Previous page" else "<#e2e8f0>Next page",
                            liveGroup = group,
                            pageControl = if (previous) PageControl.PREVIOUS else PageControl.NEXT,
                        ),
                        from = null,
                    )
                }
            }
        }
    }

    private fun rows(
        player: NpcPlayer,
        config: InventoryConfiguration,
        session: InventoryEditorSession,
        click: CanvasInput.Click,
    ) {
        val selection = session.selectionFor(config.id).toSet()
        when {
            click.shift -> {
                val delta = if (click.right) -1 else 1
                val moved = change(player, config) {
                    CanvasOps.shift(it, rows = delta, columns = 0, slots = selection.ifEmpty { null })
                }
                when {
                    !moved -> player.sendInfo("That would push items off the menu.")
                    selection.isNotEmpty() -> {
                        session.clearSelection()
                        session.select(config.id, selection.map { it + delta * 9 })
                    }
                }
            }

            click.right -> if (!change(player, config) { CanvasOps.removeRow(it) }) {
                player.sendInfo("Clear the bottom row first; one row is the minimum.")
            }

            else -> if (!change(player, config) { CanvasOps.addRow(it) }) player.sendInfo("Six rows is the maximum.")
        }
    }

    private fun handMode(player: NpcPlayer, session: InventoryEditorSession, size: Int, input: CanvasInput) {
        session.handMode = false
        if (input !is CanvasInput.Click || input.slot < size) return

        val look = surface.inventoryItem(player, input.slot - size) ?: return
        session.carried = Carried(look.copy(amount = 1), from = null)
        session.usedMaterial(look.material)
    }

    private fun livePaging(
        player: NpcPlayer,
        screen: InventoryEditorScreen.Canvas,
        config: InventoryConfiguration,
        session: InventoryEditorSession,
        input: CanvasInput,
    ) {
        if (input !is CanvasInput.Click) return
        val control = CanvasOps.itemAt(config, input.slot)?.takeIf { it.pageControl != null }
            ?: return player.sendInfo("This is the live preview; switch the toolbox's eye back to Design to edit.")
        val group = control.liveGroup ?: return
        val page = session.livePages[group] ?: 0
        val turned = if (control.pageControl == PageControl.PREVIOUS) page - 1 else page + 1
        session.livePages[group] = turned.coerceAtLeast(0)
        requestLive(player, screen)
    }

    private fun requestLive(player: NpcPlayer, screen: InventoryEditorScreen.Canvas) {
        val session = context.session(player)
        val serial = ++session.liveSerial
        session.live = session.live?.let { CanvasRender.LiveState(it.slots, loading = true, cloudDown = it.cloudDown) }
            ?: CanvasRender.LiveState(emptyMap(), loading = true, cloudDown = false)

        val pages = session.livePages.toMap()
        BackgroundTasks.launch {
            val config = context.inventoryRepository.find(screen.inventoryId) ?: return@launch
            val targets = InventoryHubMenuBuilder.usedBy(context, config.id).firstOrNull()?.targetServers.orEmpty()
            val result = try {
                Result.success(context.inventoryViews.preview(config, player, InventoryOpenContext(targets), pages))
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }

            context.runSync {
                if (session.liveSerial != serial || session.mode != CanvasMode.LIVE) return@runSync
                val preview = result.getOrNull()
                session.live = CanvasRender.LiveState(
                    preview?.slots.orEmpty(),
                    loading = false,
                    cloudDown = preview == null,
                )
                preview?.pages?.let { session.livePages.putAll(it) }
                val showing = context.isShowing(player) { it == screen }
                if (showing && !context.chatInputPrompts.isPending(player.uniqueId)) context.render(player, screen)
            }
        }
    }

    private fun openStudio(
        player: NpcPlayer,
        screen: InventoryEditorScreen.Canvas,
        session: InventoryEditorSession,
        config: InventoryConfiguration,
        slot: Int,
    ) {
        val selection = session.selectionFor(config.id)
        val slots = if (slot in selection) listOf(slot) + selection.filter { it != slot } else listOf(slot)
        context.navigate(player, InventoryEditorScreen.Studio(screen.inventoryId, slots))
    }

    private fun targets(config: InventoryConfiguration, session: InventoryEditorSession, slot: Int): List<Int> {
        val selection = session.selectionFor(config.id)
        return if (slot in selection) selection.toList() else listOf(slot)
    }

    private fun template(config: InventoryConfiguration, group: String): InventoryItemConfiguration? =
        LiveGroups.members(config, group).firstOrNull { it.pageControl == null }
            ?.deepCopy()?.copy(slot = -1, live = null, whenEmpty = null)

    private fun stillAt(config: InventoryConfiguration, slot: Int, item: InventoryItemConfiguration): Boolean =
        CanvasOps.itemAt(config, slot)?.let { CanvasOps.sameLook(it, item) } == true

    private fun change(
        player: NpcPlayer,
        config: InventoryConfiguration,
        mutate: (InventoryConfiguration) -> InventoryConfiguration?,
    ): Boolean = context.change(player, config.id, mutate)

    private companion object {
        val DECOR = InventoryItemConfiguration(material = Toolbox.DECOR_MATERIAL, name = " ", hideTooltip = true)
    }
}
