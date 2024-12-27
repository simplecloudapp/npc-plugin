package app.simplecloud.npc.shared.manager

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.hologram.HologramOptions
import app.simplecloud.npc.shared.hologram.config.HologramConfiguration
import app.simplecloud.npc.shared.namespace.NpcNamespace
import org.bukkit.Bukkit

/**
 * @author Niklas Nieberler
 */

class NpcManager(
    private val namespace: NpcNamespace
) {

    private val repository = this.namespace.npcRepository
    private val logger = Bukkit.getLogger()

    /**
     * Creates a new config if this npc does not already have one
     * @param id of the new npc
     */
    fun create(id: String) {
        if (this.repository.get(id) != null)
            return
        this.repository.save("$id.yml", createConfig(id))
        this.logger.info("[SimpleCloud-NPC] New config was created for npc $id")
    }

    /**
     * Deletes the matching npc config
     * @param id the npc where the config should be deleted
     */
    fun delete(id: String) {
        val npcConfig = this.repository.get(id) ?: return
        this.namespace.hologramManager.destroyHolograms(id)
        this.repository.delete(npcConfig)
        this.logger.info("[SimpleCloud-NPC] The config for npc $id has been deleted")
    }

    private fun createConfig(id: String): NpcConfig {
        val hologram = NpcConfig.NpcHologram(
            lores = listOf(
                HologramConfiguration("<#38bdf8><bold><group_name>"),
                HologramConfiguration("waiting for <#a3a3a3><player_count:available> players")
            )
        )
        return NpcConfig(
            id = id,
            holograms = listOf(
                hologram
            ),
            options = hashMapOf(
                HologramOptions.PLACEHOLDER_GROUP_NAME.first to "lobby"
            )
        )
    }

}