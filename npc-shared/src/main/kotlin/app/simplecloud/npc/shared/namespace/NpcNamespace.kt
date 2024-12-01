package app.simplecloud.npc.shared.namespace

import app.simplecloud.npc.shared.action.interaction.InteractionExecutor
import app.simplecloud.npc.shared.manager.NpcManager
import app.simplecloud.npc.shared.inventory.configuration.InventoryRepository
import app.simplecloud.npc.shared.repository.NpcRepository
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginManager

/**
 * @author Niklas Nieberler
 */

/**
 * This is a separate space for the loaded npc plugin on the server
 * @param pluginName will be loaded if this plugin is installed
 * @param npcIdFunction find out the matching npc id
 */
abstract class NpcNamespace<N : Any>(
    val pluginName: String,
    private val npcIdFunction: (N) -> String
) {

    val npcRepository = NpcRepository()
    val inventoryRepository = InventoryRepository()

    val npcManager = NpcManager(this)
    val interactionExecutor = InteractionExecutor(this)

    /**
     * Gets the matching id from the npc
     * @return id of the npc
     */
    fun invokeNpcId(npc: N): String {
        return this.npcIdFunction(npc)
    }

    /**
     * Is executed when namespace must be loaded
     */
    abstract fun onEnable()

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

}