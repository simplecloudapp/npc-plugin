package app.simplecloud.npc.namespace.playernpc.option

import app.simplecloud.npc.shared.option.DefaultOptions
import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.option.OptionProvider
import dev.sergiferry.playernpc.api.NPC

/**
 * @author Niklas Nieberler
 */

object PlayerNPCOptionProviders {

    /**
     * Creates a [OptionProvider] for the interacting usage of npcs
     * @param npc the npc
     */
    fun createInteractOptionProviders(npc: NPC) = OptionProvider.with(
        Option.of(DefaultOptions.NPC_ID, npc.simpleID),
        Option.of(DefaultOptions.NPC_NAME, npc.nameTag.name),
        Option.of(DefaultOptions.NPC_UUID, npc.fullID),
        Option.of(DefaultOptions.NPC_ENTITY_ID, npc.id),
    )

}