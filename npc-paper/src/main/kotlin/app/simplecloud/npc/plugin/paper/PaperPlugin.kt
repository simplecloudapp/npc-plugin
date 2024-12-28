package app.simplecloud.npc.plugin.paper

import app.simplecloud.npc.plugin.paper.command.CommandHandler
import app.simplecloud.npc.plugin.paper.namespace.NamespaceService
import app.simplecloud.npc.shared.controller.ControllerService
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

        val namespace = loadNamespace()
        CommandHandler(namespace, this).parseCommands()
    }

    private fun loadNamespace(): NpcNamespace {
        val namespace = NamespaceService.findPossibleNamespace()
        if (namespace == null) {
            logger.warning("No supported NPC software was found on the server. Find the supported plugins at https://docs.simplecloud.app/plugin/npcs#usage-of-other-npc-plugins")
            throw NullPointerException("failed to find npc namespace")
        }

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