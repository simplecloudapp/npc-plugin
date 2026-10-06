package app.simplecloud.npc.common.plugin

import app.simplecloud.api.CloudApi
import app.simplecloud.npc.common.action.InteractionExecutor
import app.simplecloud.npc.common.cloud.CloudEventHandler
import app.simplecloud.npc.common.inventory.source.InventoryDataSources
import app.simplecloud.npc.common.inventory.view.HeadOwners
import app.simplecloud.npc.common.inventory.view.InventoryViewController
import app.simplecloud.npc.common.manager.InventoryManager
import app.simplecloud.npc.common.manager.NpcManager
import app.simplecloud.npc.common.platform.Editors
import app.simplecloud.npc.common.platform.InteractionHooks
import app.simplecloud.npc.common.platform.InventoryViewRenderer
import app.simplecloud.npc.common.platform.NpcEffects
import app.simplecloud.npc.common.platform.NpcPlayerDirectory
import app.simplecloud.npc.common.platform.PlatformCatalogs
import app.simplecloud.npc.common.render.ProviderCapabilities
import app.simplecloud.npc.common.utils.InputChecks
import app.simplecloud.npc.core.inventory.InventoryRepository
import app.simplecloud.npc.core.render.HologramRenderer
import app.simplecloud.npc.core.render.NpcRenderer
import app.simplecloud.npc.core.repository.NpcRepository

class NpcPluginContext(
    val renderer: NpcRenderer,
    val hologramRenderer: HologramRenderer,
    private val inventoryViewRenderer: InventoryViewRenderer = InventoryViewRenderer.NOOP,
    val playerDirectory: NpcPlayerDirectory,
    val inventoryRepository: InventoryRepository,
    val npcRepository: NpcRepository,
    val providers: ProviderCapabilities = ProviderCapabilities.STANDALONE_ONLY,
    val catalogs: PlatformCatalogs = PlatformCatalogs.DEFAULT,
    val editors: Editors = Editors.NOOP,
    cloudApi: CloudApi? = null,
    effects: NpcEffects = NpcEffects.NONE,
) {
    @Volatile
    var hooks: InteractionHooks = InteractionHooks.NONE

    val npcManager = NpcManager(npcRepository, renderer, hologramRenderer, providers)
    val inventoryManager = InventoryManager(inventoryRepository)
    val inventoryViews: InventoryViewController = InventoryViewController(
        repository = inventoryRepository,
        renderer = inventoryViewRenderer,
        sources = InventoryDataSources.builtIns(),
        hooks = { hooks },
        executorProvider = { interactionExecutor },
    )
    val interactionExecutor: InteractionExecutor = InteractionExecutor(
        npcRepository = npcRepository,
        inventoryOpener = inventoryViews,
        effects = effects,
        hooks = { hooks },
    )
    private val cloudEvents: CloudEventHandler? = cloudApi?.let(::CloudEventHandler)

    init {
        npcRepository.setExternalChangeListener(npcManager::apply)

        inventoryRepository.onAnyChange { inventoryViews.requestRefresh() }
        HeadOwners.lookup = { owner ->
            InputChecks.parseUuid(owner)?.let { renderer.captureSkinByUuid(it) }
                ?: renderer.captureSkinByUsername(owner)
        }
        HeadOwners.onResolved = { inventoryViews.requestRefresh() }
    }

    fun runSync(task: () -> Unit) = inventoryViewRenderer.runSync(task)

    fun onEnable() {
        renderer.onEnable()
        npcRepository.loadAndWatch()
        inventoryRepository.loadAndWatch()
        cloudEvents?.registerEvents(this)
    }

    fun onDisable() {
        cloudEvents?.unregisterEvents()
        npcRepository.stopWatching()
        inventoryRepository.stopWatching()
        inventoryViews.shutdown()
        interactionExecutor.shutdown()
        hologramRenderer.destroyAll()
        renderer.onDisable()
    }
}
