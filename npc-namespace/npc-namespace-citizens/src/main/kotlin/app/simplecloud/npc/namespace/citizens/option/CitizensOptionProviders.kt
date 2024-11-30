package app.simplecloud.npc.namespace.citizens.option

import app.simplecloud.npc.shared.option.DefaultOptions
import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.option.OptionProvider
import net.citizensnpcs.api.npc.NPC

/**
 * @author Niklas Nieberler
 */

object CitizensOptionProviders {

    /**
     * Creates a [OptionProvider] for the interacting usage of npcs
     * @param npc the npc
     */
    fun createInteractOptionProviders(npc: NPC) = OptionProvider.with(
        Option.of(DefaultOptions.NPC_ID, npc.id),
        Option.of(DefaultOptions.NPC_NAME, npc.name)
    )

}