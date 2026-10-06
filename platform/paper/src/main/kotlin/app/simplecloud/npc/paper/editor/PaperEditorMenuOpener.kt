package app.simplecloud.npc.paper.editor

import app.simplecloud.npc.bukkit.scheduling.sync
import app.simplecloud.npc.common.editor.menu.EditorMenu
import app.simplecloud.npc.common.editor.menu.MenuClick
import app.simplecloud.npc.common.item.NpcItem
import app.simplecloud.npc.common.platform.EditorMenuOpener
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.paper.item.PaperNpcItem
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.plugin.Plugin
import java.util.UUID

class PaperEditorMenuOpener(private val plugin: Plugin) : EditorMenuOpener, Listener {

    private val navigating = mutableSetOf<UUID>()

    override fun runSync(task: () -> Unit) = plugin.sync(task)

    override fun runLater(delayTicks: Long, task: () -> Unit) {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable(task), delayTicks)
    }

    override fun open(
        player: NpcPlayer,
        menu: EditorMenu,
        onClick: (slot: Int, click: MenuClick) -> Unit,
        onClose: () -> Unit,
    ) {
        val bukkitPlayer = Bukkit.getPlayer(player.uniqueId) ?: return
        val current = bukkitPlayer.openInventory.topInventory
        val currentHolder = current.holder as? EditorMenuHolder
        if (currentHolder != null && current.size == menu.size && currentHolder.menu.title == menu.title) {
            currentHolder.menu = menu
            currentHolder.onClick = onClick
            currentHolder.onClose = onClose
            for (slot in 0 until menu.size) {
                current.setItem(slot, menu.items[slot]?.let(PaperNpcItem::toItemStack))
            }
            return
        }

        val holder = EditorMenuHolder(menu, onClick, onClose)
        val inventory = Bukkit.createInventory(holder, menu.size, Msg.miniMessage.deserialize(menu.title))
        holder.attach(inventory)
        menu.items.forEach { (slot, icon: NpcItem) -> inventory.setItem(slot, PaperNpcItem.toItemStack(icon)) }

        navigating.add(player.uniqueId)
        try {
            bukkitPlayer.openInventory(inventory)
        } finally {
            navigating.remove(player.uniqueId)
        }
    }

    override fun isShowing(player: NpcPlayer, menu: EditorMenu): Boolean =
        (Bukkit.getPlayer(player.uniqueId)?.openInventory?.topInventory?.holder as? EditorMenuHolder)?.menu === menu

    override fun detach(player: NpcPlayer) {
        val bukkitPlayer = Bukkit.getPlayer(player.uniqueId) ?: return
        val holder = bukkitPlayer.openInventory.topInventory.holder as? EditorMenuHolder ?: return
        holder.onClose = {}
        holder.onClick = { _, _ -> }
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        if (event.inventory.holder !is EditorMenuHolder) return
        if (event.rawSlots.any { it < event.inventory.size }) event.isCancelled = true
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val holder = event.inventory.holder as? EditorMenuHolder ?: return
        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        if (event.click == ClickType.SWAP_OFFHAND) {
            Bukkit.getScheduler().runTask(plugin, Runnable { if (player.isOnline) player.updateInventory() })
        }

        holder.onClick(event.rawSlot, PaperNpcItem.menuClickOf(event.click))
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val holder = event.inventory.holder as? EditorMenuHolder ?: return
        val bukkitPlayer = event.player as? Player ?: return
        if (bukkitPlayer.uniqueId in navigating || !plugin.isEnabled) return

        Bukkit.getScheduler().runTask(plugin, Runnable { if (bukkitPlayer.isOnline) holder.onClose() })
    }
}
