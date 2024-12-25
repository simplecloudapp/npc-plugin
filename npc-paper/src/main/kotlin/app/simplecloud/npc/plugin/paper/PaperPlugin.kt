package app.simplecloud.npc.plugin.paper

import app.simplecloud.npc.plugin.paper.command.CommandHandler
import app.simplecloud.npc.plugin.paper.namespace.NamespaceService
import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig
import app.simplecloud.npc.shared.config.NpcOption
import app.simplecloud.npc.shared.inventory.configuration.InventoryRepository
import app.simplecloud.npc.shared.namespace.NpcNamespace
import com.noxcrew.interfaces.InterfacesListeners
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Niklas Nieberler
 */

class PaperPlugin : JavaPlugin() {

    override fun onEnable() {
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        InterfacesListeners.install(this)

        val inventoryConfig = InventoryConfig(
            "test",
            null,
            listOf(
                InventoryConfig.StaticItem(
                    "test1",
                    InventoryConfig.ItemSlot(0,1),
                    InventoryConfig.ItemSlot(0,1),
                    InventoryConfig.ItemSlot(0,5)
                )
            ),
            listOf(
                InventoryConfig.InventoryItem(
                    "test1",
                    "STONE",
                    "testhallo"
                )
            )
        )


        val namespace = loadNamespace()
        CommandHandler(namespace, this).parseCommands()

        InventoryRepository().save("test.yml", inventoryConfig)
    }

    private fun loadNamespace(): NpcNamespace {
        val namespace = NamespaceService.findPossibleNamespace()
            ?: throw NullPointerException("failed to find npc namespace")
        logger.info("Load matching npc namespace: ${namespace.javaClass.simpleName}")

        val npcRepository = namespace.npcRepository
        npcRepository.load()
        namespace.inventoryRepository.load()

//        val hologramManager = namespace.hologramManager
//        hologramManager.clearLegacyHolograms()
//        hologramManager.registerFileRequest()

        namespace.onEnable()
        namespace.registerListeners(server.pluginManager, this)
        return namespace
    }

}