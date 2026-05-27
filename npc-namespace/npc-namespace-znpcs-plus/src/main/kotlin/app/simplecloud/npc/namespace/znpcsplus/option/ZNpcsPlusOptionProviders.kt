package app.simplecloud.npc.namespace.znpcsplus.option

import app.simplecloud.npc.shared.option.DefaultOptions
import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.option.OptionProvider
import lol.pyr.znpcsplus.api.npc.NpcEntry

/**
 * @author Niklas Nieberler
 */

object ZNpcsPlusOptionProviders {

    /**
     * Creates a [OptionProvider] for the interacting usage of npcs
     * @param npc the npc
     */
    fun createInteractOptionProviders(npc: NpcEntry) = OptionProvider.with(
        Option.of(DefaultOptions.NPC_ID, npc.id),
        Option.of(DefaultOptions.NPC_NAME, ""),
        Option.of(DefaultOptions.NPC_UUID, npc.npc.uuid),
        Option.of(DefaultOptions.NPC_ENTITY_ID, 0),
    )

}