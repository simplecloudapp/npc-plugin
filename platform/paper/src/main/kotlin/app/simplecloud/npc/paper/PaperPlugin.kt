package app.simplecloud.npc.paper

import app.simplecloud.npc.api.SimpleCloudNpcs
import app.simplecloud.npc.bukkit.glow.GlowTeams
import app.simplecloud.npc.bukkit.interact.InteractableEntities
import app.simplecloud.npc.bukkit.interact.NpcInteractPacketListener
import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.bukkit.interact.RealEntityClickGuard
import app.simplecloud.npc.common.command.COMMAND_LABEL
import app.simplecloud.npc.common.command.CommandMessages
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.utils.BackgroundTasks
import app.simplecloud.npc.core.cloud.CloudListCache
import app.simplecloud.npc.core.config.migration.NpcConfigMigration
import app.simplecloud.npc.core.inventory.InventoryRepository
import app.simplecloud.npc.core.repository.NpcRepository
import app.simplecloud.npc.paper.api.PaperInteractionHooks
import app.simplecloud.npc.paper.api.PaperSimpleCloudNpcs
import app.simplecloud.npc.paper.bootstrap.Listeners
import app.simplecloud.npc.paper.bootstrap.PacketStack
import app.simplecloud.npc.paper.bootstrap.ProviderReadyWait
import app.simplecloud.npc.paper.command.CommandHandler
import app.simplecloud.npc.paper.editor.EditorWiring
import app.simplecloud.npc.paper.hologram.PersonalHolograms
import app.simplecloud.npc.paper.effects.PaperNpcEffects
import app.simplecloud.npc.paper.inventory.AttachableInventoryHolder
import app.simplecloud.npc.paper.inventory.PaperInventoryViewRenderer
import app.simplecloud.npc.paper.placeholder.PlaceholderApiHook
import app.simplecloud.npc.paper.player.PaperNpcPlayer
import app.simplecloud.npc.paper.player.PaperPlayerDirectory
import app.simplecloud.npc.paper.provider.ProviderWiring
import app.simplecloud.npc.paper.pushback.PushbackListener
import app.simplecloud.npc.paper.update.UpdateChecker
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level
import kotlin.io.path.exists

class PaperPlugin : JavaPlugin() {

    private lateinit var pluginContext: NpcPluginContext
    private var editorWiring: EditorWiring? = null
    private var npcEffects: PaperNpcEffects? = null
    private var personalHolograms: PersonalHolograms? = null

    override fun onLoad() = PacketStack.load(this)

    override fun onEnable() {
        try {
            enable()
        } catch (throwable: Throwable) {
            logger.log(Level.SEVERE, "Could not start SimpleCloud NPCs; disabling it.", throwable)
            server.pluginManager.disablePlugin(this)
        }
    }

    private fun enable() {
        PacketStack.start(this)
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord")
        CommandMessages.initialize(resource("messages.yml"))
        PlaceholderApiHook.install(this)

        val npcDirectory = dataFolder.toPath().resolve("npcs")
        val onInteract: OnNpcInteract = { id, player, interaction ->
            val resolved = if (player.isSneaking) interaction.shifted() else interaction
            pluginContext.interactionExecutor.execute(id, PaperNpcPlayer(this, player), resolved)
        }
        val providers = ProviderWiring.load(this, onInteract)
        PersonalHolograms(this).also { holograms ->
            personalHolograms = holograms
            Listeners.register(this, holograms)
            holograms.start()
            providers.showPersonalText(holograms)
        }
        val inventoryViews = PaperInventoryViewRenderer(this)
        val effects = PaperNpcEffects(this).also { npcEffects = it }
        val editors = EditorWiring.create(this) { pluginContext }.also { editorWiring = it }

        pluginContext = NpcPluginContext(
            renderer = providers.routing,
            hologramRenderer = providers.holograms,
            inventoryViewRenderer = inventoryViews,
            playerDirectory = PaperPlayerDirectory(this),
            inventoryRepository = InventoryRepository(dataFolder.toPath().resolve("inventories")),
            npcRepository = NpcRepository(npcDirectory),
            providers = providers.routing,
            catalogs = PaperCatalogs,
            editors = editors.editors,
            cloudApi = CloudListCache.cloudApi,
            effects = effects,
        )
        inventoryViews.controller = pluginContext.inventoryViews
        pluginContext.hooks = PaperInteractionHooks
        UpdateChecker.check(this)
        server.servicesManager.register(
            SimpleCloudNpcs::class.java,
            PaperSimpleCloudNpcs(this) { pluginContext },
            this,
            ServicePriority.Normal,
        )

        NpcConfigMigration.backupOutdated(npcDirectory)
        if (!dataFolder.toPath().resolve("inventories").exists()) saveResource("inventories/example.yml", false)
        pluginContext.onEnable()

        GlowTeams.install(this)
        Listeners.register(this, NpcInteractPacketListener(this, onInteract), RealEntityClickGuard(), inventoryViews)
        Listeners.register(this, *editors.listeners.toTypedArray())
        Listeners.register(this, PushbackListener(pluginContext.npcRepository))
        CommandHandler(pluginContext, this).parseCommands()
        BackgroundTasks.launch { runCatching { CloudListCache.warmUp() } }

        ProviderReadyWait.schedule(this, providers.routing) {
            providers.reconcileWorlds { pluginContext.npcRepository.findAll() }
            pluginContext.npcManager.reconcileOnBoot().forEach { id ->
                logger.warning(
                    "NPC '$id' has no location yet (its previous NPC provider or provider NPC could not be " +
                        "found), so it isn't shown; stand where it should appear and run " +
                        "/$COMMAND_LABEL edit $id teleport",
                )
            }
        }
    }

    override fun onDisable() {
        PlaceholderApiHook.uninstall()
        server.servicesManager.unregisterAll(this)
        npcEffects?.shutdown()
        server.onlinePlayers.forEach { player ->
            if (player.openInventory.topInventory.holder is AttachableInventoryHolder) player.closeInventory()
        }
        if (::pluginContext.isInitialized) {
            try {
                pluginContext.onDisable()
            } catch (throwable: Throwable) {
                logger.log(Level.WARNING, "Could not cleanly shut the plugin context down", throwable)
            }
        }

        personalHolograms?.shutdown()
        BackgroundTasks.shutdown()
        CloudListCache.shutdown()
        GlowTeams.shutdown()
        InteractableEntities.clear()
        PacketStack.shutdown()
    }

    private fun resource(name: String) = dataFolder.toPath().resolve(name).also {
        if (!it.exists()) saveResource(name, false)
    }
}
