package app.simplecloud.npc.shared.namespace

import app.simplecloud.npc.shared.action.interaction.InteractionExecutor
import app.simplecloud.npc.shared.event.EventManager
import app.simplecloud.npc.shared.hologram.HologramManager
import app.simplecloud.npc.shared.inventory.InventoryManager
import app.simplecloud.npc.shared.manager.NpcManager
import app.simplecloud.npc.shared.inventory.configuration.InventoryRepository
import app.simplecloud.npc.shared.repository.NpcRepository
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

/**
 * This is a separate space for the loaded npc plugin on the server
 * @param pluginName will be loaded if this plugin is installed
 */
abstract class NpcNamespace(
    val pluginName: String
) {

    val npcRepository = NpcRepository()
    val inventoryRepository = InventoryRepository()

    val inventoryManager = InventoryManager(this)
    val eventManager = EventManager(this)
    val npcManager = NpcManager(this)
    val interactionExecutor = InteractionExecutor(this)
    val hologramManager = HologramManager(this)

    /**
     * Is executed when namespace must be loaded
     */
    open fun onEnable() {}

    /**
     * Is only executed if this namespace was loaded at startup
     */
    open fun onDisable() {}

    /**
     * Loads all required listeners for this namespace
     * @param pluginManager of the minecraft server
     * @param plugin of the npc plugin
     */
    abstract fun registerListeners(pluginManager: PluginManager, plugin: Plugin)

    /**
     * Gets all npcs
     */
    abstract fun findAllNpcs(): List<String>

    /**
     * Gets the location by a npc
     * @param id of the npc
     */
    abstract fun findLocationByNpc(id: String): Location?

}