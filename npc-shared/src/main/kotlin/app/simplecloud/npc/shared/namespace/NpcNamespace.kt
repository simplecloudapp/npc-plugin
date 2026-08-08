package app.simplecloud.npc.shared.namespace

import app.simplecloud.npc.shared.action.interaction.InteractionExecutor
import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.event.EventManager
import app.simplecloud.npc.shared.hologram.HologramManager
import app.simplecloud.npc.shared.manager.NpcManager
import app.simplecloud.npc.shared.provider.NpcProviderEvents
import app.simplecloud.npc.shared.provider.NpcProviderRegistry
import app.simplecloud.npc.shared.pushback.PushbackListener
import app.simplecloud.npc.shared.repository.NpcRepository
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.plugin.Plugin
import java.nio.file.Path

/**
 * Shared runtime for every configured NPC and every installed provider.
 *
 * Provider integrations no longer own repositories or core managers. This lets
 * Citizens and FancyNPCs configurations coexist on the same server.
 */
class NpcNamespace(
    val providerRegistry: NpcProviderRegistry,
    configDirectory: Path,
) {

    val npcRepository = NpcRepository(configDirectory, providerRegistry)
    val eventManager = EventManager(this)
    val npcManager = NpcManager(this)
    val interactionExecutor = InteractionExecutor(this)
    val hologramManager = HologramManager(this)

    init {
        npcRepository.externalDeleteListener = {
            hologramManager.destroyHolograms(it.id)
            hologramManager.destroyLegacyHolograms(it.id)
        }
    }

    fun onEnable(plugin: Plugin) {
        providerRegistry.enable(Bukkit.getPluginManager(), plugin, providerEvents())
        Bukkit.getPluginManager().registerEvents(PushbackListener(this), plugin)
    }

    fun onDisable() {
        providerRegistry.disable(Bukkit.getPluginManager())
    }

    fun findAllNpcs(): List<String> = npcRepository.findAll().map { it.id }

    fun getAvailablePlayerInteractions(): List<PlayerInteraction> = PlayerInteraction.entries

    fun existNpc(id: String): Boolean = npcRepository.find(id) != null

    fun findLocationByNpc(id: String): Location? {
        val config = npcRepository.find(id) ?: return null
        val provider = providerRegistry.getAvailable(config.provider.type, Bukkit.getPluginManager()) ?: return null
        return provider.location(config.provider.reference)
    }

    private fun providerEvents() = NpcProviderEvents(
        interact = { provider, reference, player, interaction, options ->
            val config = npcRepository.findByProvider(provider, reference) ?: return@NpcProviderEvents
            interactionExecutor.execute(config.id, player, interaction, options)
        },
        spawn = { provider, reference ->
            val config = npcRepository.findByProvider(provider, reference) ?: return@NpcProviderEvents
            hologramManager.createOrUpdate(config)
        },
        remove = { provider, reference ->
            val config = npcRepository.findByProvider(provider, reference) ?: return@NpcProviderEvents
            hologramManager.destroyHolograms(config.id)
            hologramManager.destroyLegacyHolograms(config.id)
        },
    )
}
