package app.simplecloud.npc.namespace.fancynpcs

import app.simplecloud.npc.shared.npc.NpcExecutor
import de.oliver.fancynpcs.api.FancyNpcsPlugin
import org.bukkit.Location

/**
 * @author Niklas Nieberler
 */

class FancyNpcsExecutor : NpcExecutor {

    override fun findAllNpcs(): List<String> {
        TODO("Not yet implemented")
    }

    override fun findLocationByNpc(id: String): Location? {
        TODO("Not yet implemented")
    }

    override fun setDisplayName(id: String) {
        FancyNpcsPlugin.get().npcManager.getNpc(id).data.setDisplayName("<empty>") // <empty> hides the displayName
    }

}