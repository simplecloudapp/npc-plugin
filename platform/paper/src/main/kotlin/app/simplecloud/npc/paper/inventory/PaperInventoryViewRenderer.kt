package app.simplecloud.npc.paper.inventory

import app.simplecloud.npc.bukkit.scheduling.sync
import app.simplecloud.npc.common.inventory.view.InventoryViewController
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.common.platform.InventoryViewRenderer
import app.simplecloud.npc.common.platform.RenderedInventory
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.paper.item.PaperNpcItem
import app.simplecloud.npc.paper.player.PaperNpcPlayer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InventoryViewHolder(val viewId: UUID) : AttachableInventoryHolder()

class PaperInventoryViewRenderer(private val plugin: Plugin) : InventoryViewRenderer, Listener {

    lateinit var controller: InventoryViewController

    private val inventories = ConcurrentHashMap<UUID, Inventory>()

    override fun runSync(task: () -> Unit) = plugin.sync(task)

    override fun openView(player: NpcPlayer, viewId: UUID, view: RenderedInventory): Boolean {
        val bukkitPlayer = Bukkit.getPlayer(player.uniqueId) ?: return false
        val holder = InventoryViewHolder(viewId)
        val inventory = Bukkit.createInventory(holder, view.size, Msg.miniMessage.deserialize(view.title))
        holder.attach(inventory)
        view.slots.forEach { (slot, icon) -> inventory.setItem(slot, PaperNpcItem.toItemStack(icon)) }
        inventories[viewId] = inventory
        bukkitPlayer.openInventory(inventory)

        return true
    }

    override fun updateSlots(viewId: UUID, changes: Map<Int, NpcItem?>) {
        val inventory = inventories[viewId] ?: return
        changes.forEach { (slot, icon) ->
            if (slot in 0 until inventory.size) {
                inventory.setItem(slot, icon?.let(PaperNpcItem::toItemStack))
            }
        }
    }

    override fun updateTitle(viewId: UUID, title: String) {
        val inventory = inventories[viewId] ?: return
        val legacyTitle = LEGACY.serialize(Msg.miniMessage.deserialize(title))
        inventory.viewers.toList().forEach { viewer ->
            viewer.openInventory.takeIf { it.topInventory === inventory }?.title = legacyTitle
        }
    }

    override fun closeView(viewId: UUID) {
        inventories.remove(viewId)?.let { inventory ->
            runSync { inventory.viewers.toList().forEach { it.closeInventory() } }
        }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder !is InventoryViewHolder) return
        if (event.rawSlots.any { it < event.inventory.size }) event.isCancelled = true
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? InventoryViewHolder ?: return
        event.isCancelled = true
        val slot = event.rawSlot
        if (slot !in 0 until event.inventory.size) return
        val bukkitPlayer = event.whoClicked as? Player ?: return

        val click = when (event.click) {
            ClickType.LEFT -> PlayerInteraction.LEFT_CLICK
            ClickType.SHIFT_LEFT -> PlayerInteraction.SHIFT_LEFT_CLICK
            ClickType.SHIFT_RIGHT -> PlayerInteraction.SHIFT_RIGHT_CLICK
            ClickType.RIGHT -> PlayerInteraction.RIGHT_CLICK
            else -> return
        }
        controller.handleClick(PaperNpcPlayer(plugin, bukkitPlayer), holder.viewId, slot, click)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder as? InventoryViewHolder ?: return
        inventories.remove(holder.viewId)
        if (!plugin.isEnabled) return

        Bukkit.getScheduler().runTask(plugin, Runnable { controller.viewClosed(holder.viewId) })
    }

    private companion object {
        val LEGACY: LegacyComponentSerializer = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build()
    }
}
