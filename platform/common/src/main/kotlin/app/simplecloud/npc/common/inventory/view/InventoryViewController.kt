package app.simplecloud.npc.common.inventory.view

import app.simplecloud.npc.common.action.InteractionExecutor
import app.simplecloud.npc.common.inventory.ItemClickActions
import app.simplecloud.npc.common.inventory.source.InventoryDataSources
import app.simplecloud.npc.common.inventory.source.InventoryEntry
import app.simplecloud.npc.common.inventory.source.InventoryOpenContext
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.common.platform.InteractionHooks
import app.simplecloud.npc.common.platform.InventoryViewRenderer
import app.simplecloud.npc.common.platform.RenderedInventory
import app.simplecloud.npc.common.text.PlayerMessages
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.cloud.CloudOutageLog
import app.simplecloud.npc.core.cloud.CloudUnavailableException
import app.simplecloud.npc.core.cloud.ServerBridge
import app.simplecloud.npc.core.cloud.ServerBridgeResolver
import app.simplecloud.npc.core.config.JoinStrategy
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import app.simplecloud.npc.core.inventory.InventoryRepository
import app.simplecloud.npc.core.platform.NpcPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class InventoryViewController(
    private val repository: InventoryRepository,
    private val renderer: InventoryViewRenderer,
    private val sources: InventoryDataSources,
    private val hooks: () -> InteractionHooks = { InteractionHooks.NONE },
    private val executorProvider: () -> InteractionExecutor,
) : InventoryOpener {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            logger.log(Level.WARNING, "Unhandled inventory view failure", throwable)
        },
    )
    private val views = ConcurrentHashMap<UUID, OpenView>()
    private val refreshMutex = Mutex()

    override fun open(player: NpcPlayer, inventoryId: String, context: InventoryOpenContext) {
        val history = currentView(player)?.let { it.history + (it.inventoryId to it.context) }.orEmpty()
        open(player, inventoryId, context, history, playOpenSound = true)
    }

    override fun back(player: NpcPlayer) {
        val current = currentView(player) ?: return
        val (inventoryId, context) = current.history.lastOrNull() ?: run {
            player.closeInventory()
            return
        }

        open(player, inventoryId, context, current.history.dropLast(1), playOpenSound = false)
    }

    private fun currentView(player: NpcPlayer): OpenView? =
        views.values.firstOrNull { it.player.uniqueId == player.uniqueId }

    private fun open(
        player: NpcPlayer,
        inventoryId: String,
        context: InventoryOpenContext,
        history: List<Pair<String, InventoryOpenContext>>,
        playOpenSound: Boolean,
    ) {
        scope.launch {
            val config = repository.find(inventoryId) ?: run {
                logger.warning("Cannot open inventory '$inventoryId' for ${player.name}: it does not exist.")
                return@launch
            }
            val permission = config.openPermission
            if (!permission.isNullOrBlank() && !player.hasPermission(permission)) {
                PlayerMessages.denied(player, null)
                return@launch
            }
            val view = OpenView(
                viewId = UUID.randomUUID(),
                player = player,
                inventoryId = config.id,
                context = context,
                pages = ConcurrentHashMap(),
                history = history,
            )
            val slots = computeSlots(config, view)
            val title = view.lastTitle

            view.lastSlots = slots

            val openSound = config.openSound?.takeIf { it.isNotBlank() && playOpenSound }
            renderer.runSync {
                val opened = renderer.openView(player, view.viewId, RenderedInventory(title, config.size(), slots))
                if (!opened) return@runSync
                views.values.removeIf { it.player.uniqueId == player.uniqueId }
                views[view.viewId] = view
                openSound?.let { player.playSound(it) }
            }
        }
    }

    fun handleClick(
        player: NpcPlayer,
        viewId: UUID,
        slot: Int,
        click: PlayerInteraction = PlayerInteraction.RIGHT_CLICK,
    ) {
        val view = views[viewId] ?: return
        val config = repository.find(view.inventoryId) ?: run {
            discardView(viewId)
            renderer.closeView(viewId)
            return
        }

        val liveGroups = liveGroups(config)
        if (!hooks().menuClick(config.id, slot, player, click)) return
        val cooldownKey = "menu:${view.inventoryId}:$slot:$click"

        liveGroups.forEach { (groupId, members) ->
            val control = members.firstOrNull { it.pageControl != null && it.slot == slot } ?: return@forEach
            val page = view.pages[groupId] ?: 0
            val maxPage = InventoryLayout.maxPage(contentSlots(members).size, view.lastEntryCounts[groupId] ?: 0)

            when (control.pageControl) {
                InventoryConfiguration.PageControl.PREVIOUS -> if (page > 0) {
                    view.pages[groupId] = page - 1
                    scope.launch { refreshMutex.withLock { rerender(view) } }
                }

                InventoryConfiguration.PageControl.NEXT -> if (page < maxPage) {
                    view.pages[groupId] = page + 1
                    scope.launch { refreshMutex.withLock { rerender(view) } }
                }
            }
            return
        }

        val staticItem = config.items.firstOrNull { it.slot == slot && it.liveGroup == null && visibleTo(it, player) }
        if (staticItem != null) {
            val actions = ItemClickActions.forClick(staticItem.actions, click)
            runActions(player, actions, view.context.targetServers, view.context.joinStrategy, cooldownKey)
            return
        }

        liveGroups.forEach { (groupId, members) ->
            val contentSlots = contentSlots(members)
            val slotIndex = contentSlots.indexOfFirst { it.slot == slot }
            if (slotIndex == -1) return@forEach

            val entry = view.lastEntries[groupId]?.getOrNull(slotIndex) ?: return
            val source = members.firstNotNullOfOrNull { it.live }
            val item = contentSlots[slotIndex]
            val templates = when {
                entry.overrideActions != null -> entry.overrideActions
                item.actions.isEmpty() -> source?.let { sources.find(it.type) }?.defaultEntryActions.orEmpty()
                else -> ItemClickActions.forClick(item.actions, click)
            }
            val actions = templates.map { EntryActions.substituted(it, entry.substitutions) }
            val targets = entry.targetOverride.ifEmpty { view.context.targetServers }

            runActions(player, actions, targets, view.context.joinStrategy, cooldownKey)
            return
        }
    }

    class Preview(val slots: Map<Int, NpcItem>, val pages: Map<String, Int>, val entryCounts: Map<String, Int>)

    suspend fun preview(
        config: InventoryConfiguration,
        player: NpcPlayer,
        context: InventoryOpenContext,
        pages: Map<String, Int>,
    ): Preview {
        val view = OpenView(UUID.randomUUID(), player, config.id, context, ConcurrentHashMap(pages))
        val slots = computeSlots(config, view)

        return Preview(slots, view.pages.toMap(), view.lastEntryCounts.toMap())
    }

    fun viewClosed(viewId: UUID) = discardView(viewId)

    private fun discardView(viewId: UUID) {
        views.remove(viewId)
    }

    fun dataSourceTypes(): List<String> = sources.types()

    fun requestRefresh() {
        scope.launch { refreshOpenViews() }
    }

    suspend fun refreshOpenViews() = refreshMutex.withLock {
        views.values.forEach { view ->
            try {
                rerender(view)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                logger.warning(
                    "Could not refresh inventory view '${view.inventoryId}' for ${view.player.name}: " +
                        exception.message,
                )
            }
        }
    }

    fun shutdown() {
        scope.cancel()
        views.keys.toList().forEach { viewId -> runCatching { renderer.closeView(viewId) } }
        views.clear()
    }

    private suspend fun rerender(view: OpenView) {
        val config = repository.find(view.inventoryId) ?: run {
            discardView(view.viewId)
            renderer.closeView(view.viewId)
            return
        }
        val previousTitle = view.lastTitle
        val slots = computeSlots(config, view)
        val changes = InventoryLayout.diff(view.lastSlots, slots)
        val title = view.lastTitle.takeIf { it != previousTitle }

        view.lastSlots = slots
        if (changes.isNotEmpty() || title != null) {
            renderer.runSync {
                title?.let { renderer.updateTitle(view.viewId, it) }
                if (changes.isNotEmpty()) renderer.updateSlots(view.viewId, changes)
            }
        }
    }

    private suspend fun computeSlots(config: InventoryConfiguration, view: OpenView): Map<Int, NpcItem> {
        val slots = mutableMapOf<Int, NpcItem>()
        val size = config.size()
        val viewer = view.player.uniqueId
        var pages: Pair<Int, Int>? = null

        liveGroups(config).forEach { (groupId, members) ->
            val contentSlots = contentSlots(members)
            val sourceConfig = members.firstNotNullOfOrNull { it.live } ?: run {
                logger.warning(
                    "Inventory '${config.id}' live-group '$groupId' has no `live` source configured, skipping.",
                )
                return@forEach
            }
            val source = sources.find(sourceConfig.type) ?: run {
                logger.warning(
                    "Inventory '${config.id}' live-group '$groupId' uses unknown source type " +
                        "'${sourceConfig.type}', skipping.",
                )
                return@forEach
            }

            val entries = try {
                source.entries(sourceConfig, view.context)
            } catch (exception: CloudUnavailableException) {
                CloudOutageLog.warnThrottled(
                    "Inventory '${config.id}' shows no live entries: ${exception.message}",
                    key = "inventory:${config.id}",
                )
                emptyList()
            }
            val maxPage = InventoryLayout.maxPage(contentSlots.size, entries.size)
            val requested = view.pages[groupId] ?: 0
            val page = requested.coerceIn(0, maxPage)
            if (page != requested) view.pages[groupId] = page
            if (pages == null) pages = page + 1 to maxPage + 1
            val window = InventoryLayout.pageWindow(entries, contentSlots.size, page)
            view.lastEntries[groupId] = window
            view.lastEntryCounts[groupId] = entries.size

            val whenEmpty = members.firstNotNullOfOrNull { it.whenEmpty }
            contentSlots.forEachIndexed { index, item ->
                val entry = window.getOrNull(index)
                if (entry != null) {
                    slots[item.slot] = ItemIconResolver.liveIcon(item, entry, sourceConfig.lookFor(entry.state), viewer)
                } else {
                    whenEmpty?.let { slots[item.slot] = ItemIconResolver.icon(it, viewer) }
                }
            }

            if (maxPage > 0) {
                members.filter { it.pageControl == InventoryConfiguration.PageControl.PREVIOUS }.forEach {
                    if (page > 0) slots[it.slot] = ItemIconResolver.icon(it, viewer)
                }
                members.filter { it.pageControl == InventoryConfiguration.PageControl.NEXT }.forEach {
                    if (page < maxPage) slots[it.slot] = ItemIconResolver.icon(it, viewer)
                }
            }
        }

        config.items
            .filter { it.slot in 0 until size && it.liveGroup == null && visibleTo(it, view.player) }
            .forEach { item -> slots.putIfAbsent(item.slot, ItemIconResolver.icon(item, viewer)) }

        val text = MenuText(view.player, targetOf(view), pages)
        view.lastTitle = text.resolve(config.title)

        return slots.mapValues { (_, item) ->
            item.copy(name = text.resolve(item.name), lore = item.lore.map { text.resolve(it) })
        }
    }

    private suspend fun targetOf(view: OpenView): ServerBridge? {
        val target = view.context.targetServers.firstOrNull() ?: return null
        return try {
            ServerBridgeResolver.resolve(target)
        } catch (_: CloudUnavailableException) {
            null
        }
    }

    private fun visibleTo(item: InventoryItemConfiguration, player: NpcPlayer): Boolean =
        item.viewPermission.let { it.isNullOrBlank() || player.hasPermission(it) }

    private fun liveGroups(config: InventoryConfiguration): Map<String, List<InventoryItemConfiguration>> {
        val size = config.size()
        return config.items.filter { it.liveGroup != null && it.slot in 0 until size }.groupBy { it.liveGroup!! }
    }

    private fun contentSlots(members: List<InventoryItemConfiguration>) =
        members.filter { it.pageControl == null }.sortedBy { it.slot }

    private fun runActions(
        player: NpcPlayer,
        actions: List<NpcConfig.ActionConfiguration>,
        targetServers: List<String>,
        strategy: JoinStrategy,
        cooldownKey: String,
    ) {
        if (actions.isEmpty()) return
        val executor = executorProvider()
        if (!executor.passesCooldown(player, cooldownKey, actions)) return

        scope.launch {
            actions.forEach { executor.executeAction(targetServers, it, player, strategy) }
        }
    }

    private class OpenView(
        val viewId: UUID,
        val player: NpcPlayer,
        val inventoryId: String,
        val context: InventoryOpenContext,
        val pages: ConcurrentHashMap<String, Int>,
        val history: List<Pair<String, InventoryOpenContext>> = emptyList(),
    ) {
        @Volatile
        var lastSlots: Map<Int, NpcItem> = emptyMap()

        @Volatile
        var lastTitle: String = ""

        val lastEntries = ConcurrentHashMap<String, List<InventoryEntry>>()
        val lastEntryCounts = ConcurrentHashMap<String, Int>()
    }

    private companion object {
        private val logger = NpcLog.logger
    }
}
