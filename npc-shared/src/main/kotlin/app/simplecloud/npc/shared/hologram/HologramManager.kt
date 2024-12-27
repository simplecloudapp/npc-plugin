package app.simplecloud.npc.shared.hologram

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.createAtNamespacedKey
import app.simplecloud.npc.shared.hologram.config.HologramConfiguration
import app.simplecloud.npc.shared.hologram.placeholder.HologramPlaceholderHandler
import app.simplecloud.npc.shared.hologramNamespacedKey
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.sync
import app.simplecloud.npc.shared.utils.NpcFileUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.entity.TextDisplay
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

/**
 * @author Niklas Nieberler
 */

class HologramManager(
    private val namespace: NpcNamespace
) {

    private val placeholder = HologramPlaceholderHandler(namespace)
    private val textDisplays = hashMapOf<UUID, String>()

    /**
     * Clears all legacy hologram entities
     */
    fun clearLegacyHolograms() {
        this.namespace.findAllNpcs().forEach {
            destroyLegacyHolograms(it)
        }
    }

    /**
     * Registers a new file request for the npc config files
     */
    fun registerFileRequest() {
        NpcFileUpdater.updateFileRequest("hologram") {
            CoroutineScope(Dispatchers.IO).launch { updateHolograms(it) }
        }
    }

    /**
     * Updates all holograms by a npc
     * @param npcConfig of npc
     */
    suspend fun updateHolograms(npcConfig: NpcConfig) {
        destroyHolograms(npcConfig.id)
        val joinState = JoinStateHelper.getJoinState(npcConfig)
        val hologram = npcConfig.getHologram(joinState)
        sync { updateHologram(npcConfig.id, hologram) }
    }

    /**
     * Updates a single hologram state
     * @param id of the npc
     * @param hologram state to update
     */
    fun updateHologram(id: String, hologram: NpcConfig.NpcHologram) {
        val location = this.namespace.findLocationByNpc(id)
            ?: throw NullPointerException("failed to find location")
        var hologramEditor = HologramEditor(id, location.clone().add(0.0, hologram.startHeight, 0.0))
        hologram.lores.reversed().forEach {
            modifyHologram(id, it, hologramEditor)
            val component = this.placeholder.syncPlaceholderComponent(id, it.text)
            hologramEditor = hologramEditor.withCustomName(component)
                .withNextLine()
            modifyHologram(id, it, hologramEditor)
        }
        hologramEditor.destroy()
    }

    /**
     * Updates from a npc only the hologram texts
     * @param id of the npc
     * @param lores new hologram lores
     */
    suspend fun updateTextHologram(id: String, lores: List<String>) {
        lores.forEachIndexed { index, newText ->
            val component = this.placeholder.placeholderComponent(id, newText)
            sync {
                val displays = getTextDisplays(id)
                    .sortedBy { it.persistentDataContainer.get(createAtNamespacedKey, PersistentDataType.LONG) }
                displays[index].text(component)
            }
        }
    }

    /**
     * Updates from a npc only the hologram texts
     * @param config of the npc
     * @param name of the group
     */
    suspend fun updateTextHologramByGroup(config: NpcConfig, name: String) {
        val joinState = JoinStateHelper.getJoinState(name)
        updateTextHologram(config, joinState)
    }

    /**
     * Updates from a npc only the hologram texts
     * @param config of the npc
     * @param state of the join state
     */
    suspend fun updateTextHologram(config: NpcConfig, state: String?) {
        val hologram = config.getHologram(state)
        updateTextHologram(config.id, hologram.lores.reversed().map { it.text })
    }

    /**
     * Destroys all holograms by a npc
     * @param id of the npc
     */
    fun destroyHolograms(id: String) {
        sync { getTextDisplays(id).forEach { it.remove() } }
    }

    /**
     * Destroys all legacy holograms by a npc
     * @param id of the npc
     */
    fun destroyLegacyHolograms(id: String) {
        val location = this.namespace.findLocationByNpc(id)
            ?: throw NullPointerException("failed to find location")
        location.world.entities
            .filter { it.persistentDataContainer.get(hologramNamespacedKey, PersistentDataType.STRING) == id }
            .forEach { it.remove() }
    }

    private fun modifyHologram(id: String, configuration: HologramConfiguration, hologramEditor: HologramEditor) {
        this.textDisplays[hologramEditor.id] = id
        HologramModifier.modify(configuration, hologramEditor.textDisplay)
    }

    private fun getTextDisplays(id: String): List<TextDisplay> {
        return this.textDisplays.filterValues { it == id }
            .mapNotNull { Bukkit.getEntity(it.key) }
            .filterIsInstance<TextDisplay>()
    }
}