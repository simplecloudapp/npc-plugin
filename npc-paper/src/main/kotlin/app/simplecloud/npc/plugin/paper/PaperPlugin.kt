package app.simplecloud.npc.plugin.paper

import app.simplecloud.npc.plugin.paper.command.CommandHandler
import app.simplecloud.npc.plugin.paper.namespace.NamespaceService
import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.inventory.configuration.InventoryConfig
import app.simplecloud.npc.shared.config.NpcOption
import app.simplecloud.npc.shared.controller.ControllerService
import app.simplecloud.npc.shared.inventory.configuration.InventoryRepository
import app.simplecloud.npc.shared.namespace.NpcNamespace
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Niklas Nieberler
 */

class PaperPlugin : JavaPlugin() {

    override fun onEnable() {
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        val inventoryConfig = InventoryConfig(
            "test",
            InventoryConfig.PaginationInventory(
                InventoryConfig.ItemSlot(0, 0),
                InventoryConfig.ItemSlot(3, 8),
                "test1",
                "test2"
            ),
            listOf(
                InventoryConfig.StaticItem(
                    "asd",
                    InventoryConfig.ItemSlot(0,1)
                ),
                InventoryConfig.StaticItem(
                    "moin",
                    InventoryConfig.ItemSlot(0,1),
                    InventoryConfig.ItemSlot(0,8)
                )
            ),
            listOf(
                InventoryConfig.InventoryItem(
                    "test1",
                    "STONE",
                    "testhallo",
                    Action.RUN_COMMAND,
                    listOf(
                        NpcOption("2", "1")
                    )
                )
            )
        )

        InventoryRepository().save("test.yml", inventoryConfig)

        val namespace = loadNamespace()
        CommandHandler(namespace, this).parseCommands()
    }

    private fun loadNamespace(): NpcNamespace {
        val namespace = NamespaceService.findPossibleNamespace()
            ?: throw NullPointerException("failed to find npc namespace")
        logger.info("Load matching npc namespace: ${namespace.javaClass.simpleName}")

        ControllerService.eventHandler.registerEvents(namespace)

        val npcRepository = namespace.npcRepository
        npcRepository.load()
        namespace.inventoryRepository.load()

        val hologramManager = namespace.hologramManager
        hologramManager.clearLegacyHolograms()
        hologramManager.registerFileRequest()

        namespace.onEnable()
        namespace.registerListeners(server.pluginManager, this)
        return namespace
    }

}