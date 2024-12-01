package app.simplecloud.npc.plugin.paper

import app.simplecloud.npc.plugin.paper.namespace.NamespaceService
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Niklas Nieberler
 */

class PaperPlugin : JavaPlugin() {

    override fun onEnable() {
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        loadNamespace()
    }

    private fun loadNamespace() {
        val namespace = NamespaceService.findPossibleNamespace()
            ?: throw NullPointerException("failed to find npc namespace")
        logger.info("Load matching npc namespace: ${namespace.javaClass.simpleName}")

        val npcRepository = namespace.npcRepository
        npcRepository.load()

        namespace.onEnable()
        namespace.registerListeners(server.pluginManager, this)
    }

}