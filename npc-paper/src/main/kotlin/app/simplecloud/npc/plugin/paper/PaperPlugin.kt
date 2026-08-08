package app.simplecloud.npc.plugin.paper

import app.simplecloud.npc.plugin.paper.command.CommandHandler
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.plugin.paper.namespace.NamespaceService
import app.simplecloud.npc.shared.cloud.CloudService
import app.simplecloud.npc.shared.config.NpcConfigMigration
import app.simplecloud.npc.shared.namespace.NpcNamespace
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class PaperPlugin : JavaPlugin() {

    private var namespace: NpcNamespace? = null

    override fun onEnable() {
        try {
            when (val result = PluginDataFolderMigration.migrate(dataFolder.toPath())) {
                PluginDataFolderMigration.Result.None -> Unit
                PluginDataFolderMigration.Result.Moved -> logger.info(
                    "Migrated the plugin data folder from ${PluginDataFolderMigration.LEGACY_FOLDER_NAME} to ${dataFolder.name}."
                )
                PluginDataFolderMigration.Result.Merged -> logger.info(
                    "Merged the legacy ${PluginDataFolderMigration.LEGACY_FOLDER_NAME} data into ${dataFolder.name}."
                )
                is PluginDataFolderMigration.Result.MergedWithConflicts -> logger.warning(
                    "Merged legacy plugin data, but kept conflicting files in ${PluginDataFolderMigration.LEGACY_FOLDER_NAME}: " +
                        result.paths.joinToString()
                )
            }
        } catch (exception: Exception) {
            logger.log(Level.SEVERE, "Could not migrate the legacy plugin data folder; disabling to protect its data.", exception)
            server.pluginManager.disablePlugin(this)
            return
        }

        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        CommandMessages.initialize(this)

        val registry = NamespaceService.createProviderRegistry()
        val availableProviders = registry.availableProviders(server.pluginManager)
        if (availableProviders.isEmpty()) {
            logger.warning("No supported NPC provider is installed. Install Citizens, FancyNPCs, ZNPCsPlus, or MythicMobs.")
            server.pluginManager.disablePlugin(this)
            return
        }

        logger.info("Available NPC providers: ${availableProviders.joinToString { it.type.commandName }}")

        val npcDirectory = dataFolder.toPath().resolve("npcs")
        val namespace = NpcNamespace(registry, npcDirectory)
        this.namespace = namespace
        namespace.onEnable(this)
        NpcConfigMigration.backupOutdated(npcDirectory)
        namespace.npcRepository.loadAndWatch()
        namespace.hologramManager.registerFileRequest()
        CloudService.eventHandler.registerEvents(namespace)
        CommandHandler(namespace, this).parseCommands()

        namespace.npcRepository.findAll().forEach(namespace.hologramManager::createOrUpdate)
    }

    override fun onDisable() {
        CloudService.eventHandler.unregisterEvents()
        namespace?.apply {
            npcRepository.stopWatching()
            hologramManager.destroyAllHolograms()
            onDisable()
        }
    }
}
