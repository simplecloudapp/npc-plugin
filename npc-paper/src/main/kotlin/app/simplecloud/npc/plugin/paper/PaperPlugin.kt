package app.simplecloud.npc.plugin.paper

import app.simplecloud.npc.plugin.paper.namespace.NamespaceService
import app.simplecloud.npc.shared.action.Action
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.repository.NpcRepository
import org.bukkit.plugin.java.JavaPlugin

/**
 * @author Niklas Nieberler
 */

class PaperPlugin : JavaPlugin() {

    override fun onEnable() {
        loadNamespace()

        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")

        val npcConfig = NpcConfig(
            "test",
            mutableListOf(
                NpcConfig.NpcInteraction(
                    PlayerInteraction.RIGHT_CLICK,
                    Action.OPEN_INVENTORY,
                    listOf(
                        NpcConfig.NpcOption("2", "1")
                    )
                )
            ),
        )

        // NpcRepository().save("test.yml", npcConfig)
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