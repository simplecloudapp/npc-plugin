package app.simplecloud.npc.shared.npc

import org.bukkit.Location

/**
 * @author Niklas Nieberler
 */

interface NpcExecutor { // TODO: better name

    /**
     * Gets all npcs
     */
    fun findAllNpcs(): List<String>

    /**
     * Gets the location by a npc
     * @param id of the npc
     */
    fun findLocationByNpc(id: String): Location?

    fun setDisplayName(id: String)

}