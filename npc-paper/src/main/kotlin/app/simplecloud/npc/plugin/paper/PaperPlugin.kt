package app.simplecloud.npc.plugin.paper

import app.simplecloud.npc.plugin.paper.command.CommandHandler
import app.simplecloud.npc.plugin.paper.namespace.NamespaceService
import app.simplecloud.npc.shared.cloud.CloudService
import app.simplecloud.npc.shared.namespace.NpcNamespace
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Niklas Nieberler
 */

class PaperPlugin : JavaPlugin() {

    private var namespace: NpcNamespace? = null

    override fun onEnable() {
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        val namespace = loadNamespace() ?: return
        CommandHandler(namespace, this).parseCommands()
    }

    override fun onDisable() {
        val namespace = this.namespace ?: return
        namespace.npcRepository.stopWatching()
        namespace.hologramManager.destroyAllHolograms()
    }

    private fun loadNamespace(): NpcNamespace? {
        val namespace = NamespaceService.findPossibleNamespace()
        if (namespace == null) {
            logger.warning("No supported NPC software was found on the server. Find the supported plugins at https://docs.simplecloud.app/manual/plugins/npcs#required-npc-plugins")
            server.pluginManager.disablePlugin(this)
            return null
        }

        logger.info("Load matching npc namespace: ${namespace.javaClass.simpleName}")

        CloudService.eventHandler.registerEvents(namespace)

        val npcRepository = namespace.npcRepository
        npcRepository.loadAndWatch()

        val hologramManager = namespace.hologramManager
        hologramManager.registerFileRequest()

        namespace.onEnable()
        namespace.registerListeners(server.pluginManager, this)
        this.namespace = namespace
        return namespace
    }

}
