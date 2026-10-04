package app.simplecloud.npc.common.editor

import app.simplecloud.npc.common.editor.core.TextPrompts
import app.simplecloud.npc.common.inventory.view.InventoryViewController
import app.simplecloud.npc.common.manager.NpcManager
import app.simplecloud.npc.common.platform.Editors
import app.simplecloud.npc.common.platform.NpcPlayerDirectory
import app.simplecloud.npc.common.platform.PlatformCatalogs
import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.render.ProviderCapabilities
import app.simplecloud.npc.core.inventory.InventoryRepository
import app.simplecloud.npc.core.render.HologramRenderer
import app.simplecloud.npc.core.render.NpcRenderer
import app.simplecloud.npc.core.repository.NpcRepository

open class EditorPorts(
    private val pluginContextProvider: () -> NpcPluginContext,
    val textPrompts: TextPrompts,
    val runSync: (() -> Unit) -> Unit,
    val runLater: (delayTicks: Long, () -> Unit) -> Unit,
) {
    val npcRepository: NpcRepository get() = pluginContextProvider().npcRepository
    val inventoryRepository: InventoryRepository get() = pluginContextProvider().inventoryRepository
    val renderer: NpcRenderer get() = pluginContextProvider().renderer
    val hologramRenderer: HologramRenderer get() = pluginContextProvider().hologramRenderer
    val npcManager: NpcManager get() = pluginContextProvider().npcManager
    val playerDirectory: NpcPlayerDirectory get() = pluginContextProvider().playerDirectory
    val inventoryViews: InventoryViewController get() = pluginContextProvider().inventoryViews
    val catalogs: PlatformCatalogs get() = pluginContextProvider().catalogs
    val providers: ProviderCapabilities get() = pluginContextProvider().providers
    val editors: Editors get() = pluginContextProvider().editors
}
