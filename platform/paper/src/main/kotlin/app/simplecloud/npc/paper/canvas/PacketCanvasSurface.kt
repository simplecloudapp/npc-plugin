package app.simplecloud.npc.paper.canvas

import app.simplecloud.npc.bukkit.equipment.ArmorLooks
import app.simplecloud.npc.common.editor.canvas.CanvasInput
import app.simplecloud.npc.common.editor.canvas.CanvasSurface
import app.simplecloud.npc.common.editor.canvas.CanvasView
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.inventory.InventoryConfiguration.InventoryItemConfiguration
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.paper.inventory.AttachableInventoryHolder
import app.simplecloud.npc.paper.item.PaperNpcItem
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack as BukkitItemStack
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PacketCanvasSurface(private val plugin: Plugin) : PacketListenerAbstract(), CanvasSurface, Listener {

    private class Holder : AttachableInventoryHolder()

    private class Session(
        val holder: Holder,
        val title: String,
        val size: Int,
        @Volatile var toolbox: List<ItemStack>,
        @Volatile var showInventory: Boolean,
        @Volatile var carried: ItemStack,
        @Volatile var onInput: (CanvasInput) -> Unit,
        @Volatile var onClose: () -> Unit,
    ) {
        @Volatile
        var windowId = -1
        val decoder = CanvasClickDecoder()
    }

    private val sessions = ConcurrentHashMap<UUID, Session>()

    private val awaitingResync = ConcurrentHashMap.newKeySet<UUID>()

    override fun open(player: NpcPlayer, view: CanvasView, onInput: (CanvasInput) -> Unit, onClose: () -> Unit) {
        val bukkitPlayer = Bukkit.getPlayer(player.uniqueId) ?: return
        val toolbox = toolboxStacks(view.toolbox)
        val carried = view.carried?.let(::packetStack) ?: ItemStack.EMPTY

        val current = sessions[player.uniqueId]
        val showing = bukkitPlayer.openInventory.topInventory
        val sameWindow = current != null &&
            showing.holder === current.holder &&
            current.size == view.size &&
            current.title == view.title
        if (sameWindow) {
            current.toolbox = toolbox
            current.showInventory = view.showInventory
            current.carried = carried
            current.onInput = onInput
            current.onClose = onClose
            for (slot in 0 until view.size) showing.setItem(slot, view.items[slot]?.let(PaperNpcItem::toItemStack))
            bukkitPlayer.updateInventory()
            return
        }

        val holder = Holder()
        val inventory = Bukkit.createInventory(holder, view.size, Msg.miniMessage.deserialize(view.title))
        holder.attach(inventory)
        view.items.forEach { (slot, item) -> inventory.setItem(slot, PaperNpcItem.toItemStack(item)) }

        sessions[player.uniqueId] =
            Session(holder, view.title, view.size, toolbox, view.showInventory, carried, onInput, onClose)
        awaitingResync += player.uniqueId
        bukkitPlayer.openInventory(inventory)
    }

    override fun detach(player: NpcPlayer) {
        if (sessions.remove(player.uniqueId) != null) scheduleResync(player.uniqueId)
    }

    override fun onPacketReceive(event: PacketReceiveEvent) {
        val uuid = event.user.uuid ?: return
        when (event.packetType) {
            PacketType.Play.Client.CLICK_WINDOW -> {
                val session = sessions[uuid] ?: return
                val click = WrapperPlayClientClickWindow(event)
                if (click.windowId != session.windowId) return

                event.isCancelled = true
                val input = session.decoder.decode(click.windowClickType, click.button, click.slot)
                runOnMain(uuid) { player ->
                    if (sessions[uuid] !== session) return@runOnMain
                    try {
                        input?.let(session.onInput)
                    } finally {
                        if (sessions[uuid] === session) player.updateInventory()
                    }
                }
            }

            PacketType.Play.Client.CREATIVE_INVENTORY_ACTION ->
                if (sessions.containsKey(uuid) || uuid in awaitingResync) event.isCancelled = true

            else -> Unit
        }
    }

    override fun onPacketSend(event: PacketSendEvent) {
        val uuid = event.user.uuid ?: return
        val session = sessions[uuid] ?: return

        when (event.packetType) {
            PacketType.Play.Server.OPEN_WINDOW -> if (session.windowId == -1) {
                session.windowId = WrapperPlayServerOpenWindow(event).containerId
            }

            PacketType.Play.Server.WINDOW_ITEMS -> {
                val packet = WrapperPlayServerWindowItems(event)
                when (packet.windowId) {
                    session.windowId -> {
                        if (!session.showInventory) {
                            packet.items = packet.items.toMutableList().also { items ->
                                session.toolbox.forEachIndexed { index, stack ->
                                    if (session.size + index < items.size) items[session.size + index] = stack
                                }
                            }
                        }
                        packet.setCarriedItem(session.carried)
                        event.markForReEncode(true)
                    }

                    PLAYER_WINDOW -> if (!session.showInventory) {
                        packet.items = packet.items.toMutableList().also { items ->
                            for (slot in PLAYER_WINDOW_STORAGE) {
                                if (slot < items.size) items[slot] = toolboxForPlayerWindow(session, slot)
                            }
                        }
                        event.markForReEncode(true)
                    }
                }
            }

            PacketType.Play.Server.SET_SLOT -> {
                val packet = WrapperPlayServerSetSlot(event)
                val replacement = when {
                    session.showInventory && !(packet.windowId == CURSOR_WINDOW && packet.slot == CURSOR_SLOT) -> null
                    packet.windowId == session.windowId && packet.slot >= session.size ->
                        session.toolbox.getOrNull(packet.slot - session.size)

                    packet.windowId == PLAYER_WINDOW && packet.slot in PLAYER_WINDOW_STORAGE ->
                        toolboxForPlayerWindow(session, packet.slot)

                    packet.windowId == CURSOR_WINDOW && packet.slot == CURSOR_SLOT -> session.carried
                    else -> null
                } ?: return
                packet.item = replacement
                event.markForReEncode(true)
            }

            PacketType.Play.Server.SET_PLAYER_INVENTORY -> {
                if (session.showInventory) return
                val packet = WrapperPlayServerSetPlayerInventory(event)
                val index = toolboxIndexForInventorySlot(packet.slot) ?: return
                packet.stack = session.toolbox.getOrNull(index) ?: return
                event.markForReEncode(true)
            }

            PacketType.Play.Server.SET_CURSOR_ITEM -> {
                WrapperPlayServerSetCursorItem(event).stack = session.carried
                event.markForReEncode(true)
            }

            else -> Unit
        }
    }

    override fun inventoryItem(player: NpcPlayer, index: Int): InventoryItemConfiguration? =
        stackAt(player, index)?.let(ItemLooks::of)

    override fun equipmentItem(player: NpcPlayer, index: Int): NpcConfig.EquipmentItem? =
        stackAt(player, index)?.let(ArmorLooks::read)

    private fun stackAt(player: NpcPlayer, index: Int): BukkitItemStack? {
        val inventory = Bukkit.getPlayer(player.uniqueId)?.inventory ?: return null
        val stack = when (index) {
            in 0..26 -> inventory.getItem(index + 9)
            in 27..35 -> inventory.getItem(index - 27)
            CanvasInput.OFFHAND_KEY -> inventory.itemInOffHand
            else -> return null
        }

        return stack?.takeUnless { it.type.isAir }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val uuid = event.player.uniqueId
        val session = sessions[uuid] ?: return
        if (event.inventory.holder !== session.holder) return

        sessions.remove(uuid, session)
        scheduleResync(uuid)
        val player = event.player
        Bukkit.getScheduler().runTask(plugin, Runnable { if (player is Player && player.isOnline) session.onClose() })
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        sessions.remove(event.player.uniqueId)
        awaitingResync.remove(event.player.uniqueId)
    }

    private fun scheduleResync(uuid: UUID) {
        awaitingResync += uuid
        runOnMain(uuid) { player ->
            if (sessions.containsKey(uuid)) return@runOnMain
            player.updateInventory()
            awaitingResync.remove(uuid)
        }
    }

    private fun runOnMain(uuid: UUID, task: (Player) -> Unit) {
        if (!plugin.isEnabled) return
        Bukkit.getScheduler().runTask(plugin, Runnable { Bukkit.getPlayer(uuid)?.let(task) })
    }

    private fun toolboxStacks(toolbox: Map<Int, NpcItem>): List<ItemStack> =
        List(CanvasView.TOOLBOX_SIZE) { index -> toolbox[index]?.let(::packetStack) ?: ItemStack.EMPTY }

    private fun packetStack(item: NpcItem): ItemStack =
        SpigotConversionUtil.fromBukkitItemStack(PaperNpcItem.toItemStack(item))

    private fun toolboxForPlayerWindow(session: Session, slot: Int): ItemStack =
        session.toolbox.getOrNull(slot - PLAYER_WINDOW_STORAGE.first) ?: ItemStack.EMPTY

    private fun toolboxIndexForInventorySlot(slot: Int): Int? = when (slot) {
        in 0..8 -> 27 + slot
        in 9..35 -> slot - 9
        else -> null
    }

    private companion object {
        const val PLAYER_WINDOW = 0
        const val CURSOR_WINDOW = -1
        const val CURSOR_SLOT = -1
        val PLAYER_WINDOW_STORAGE = 9..44
    }
}
