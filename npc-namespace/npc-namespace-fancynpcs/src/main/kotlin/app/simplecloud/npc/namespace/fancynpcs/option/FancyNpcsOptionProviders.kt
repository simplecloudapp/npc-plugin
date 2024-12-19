package app.simplecloud.npc.namespace.fancynpcs.option

import app.simplecloud.npc.shared.option.DefaultOptions
import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.option.OptionProvider
import de.oliver.fancynpcs.api.Npc

/**
 * @author Niklas Nieberler
 */

object FancyNpcsOptionProviders {

    /**
     * Creates a [OptionProvider] for the interacting usage of npcs
     * @param npc the npc
     */
    fun createInteractOptionProviders(npc: Npc) = OptionProvider.with(
        Option.of(DefaultOptions.NPC_ID, npc.data.name),
        Option.of(DefaultOptions.NPC_NAME, npc.data.name),
        Option.of(DefaultOptions.NPC_UUID, npc.data.id),
        Option.of(DefaultOptions.NPC_ENTITY_ID, npc.entityId),
    )

}