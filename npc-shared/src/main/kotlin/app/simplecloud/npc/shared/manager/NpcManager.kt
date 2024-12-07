package app.simplecloud.npc.shared.manager

import app.simplecloud.npc.shared.config.NpcConfig
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
        this.repository.save("$id.yml", NpcConfig(id))
        this.logger.info("[SimpleCloud-NPC] New config was created for npc $id")
    }

    /**
     * Deletes the matching npc config
     * @param id the npc where the config should be deleted
     */
    fun delete(id: String) {
        val npcConfig = this.repository.get(id) ?: return
        this.repository.delete(npcConfig)
        this.logger.info("[SimpleCloud-NPC] The config for npc $id has been deleted")
    }

}