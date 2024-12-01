package app.simplecloud.npc.shared.manager

import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.namespace.NpcNamespace
import org.bukkit.Bukkit

/**
 * @author Niklas Nieberler
 */

class NpcManager<N : Any>(
    private val namespace: NpcNamespace<N>
) {

    private val repository = this.namespace.npcRepository
    private val logger = Bukkit.getLogger()

    /**
     * Creates a new config if this npc does not already have one
     * @param npc the new npc
     */
    fun create(npc: N) {
        val npcId = this.namespace.invokeNpcId(npc)
        if (this.repository.get(npcId) != null)
            return
        this.repository.save("$npcId.yml", NpcConfig(npcId))
        this.logger.info("[SimpleCloud-NPC] New config was created for npc $npcId")
    }

    /**
     * Deletes the matching npc config
     * @param npc the npc where the config should be deleted
     */
    fun delete(npc: N) {
        val npcId = this.namespace.invokeNpcId(npc)
        val npcConfig = this.repository.get(npcId) ?: return
        this.repository.delete(npcConfig)
        this.logger.info("[SimpleCloud-NPC] The config for npc $npcId has been deleted")
    }

}