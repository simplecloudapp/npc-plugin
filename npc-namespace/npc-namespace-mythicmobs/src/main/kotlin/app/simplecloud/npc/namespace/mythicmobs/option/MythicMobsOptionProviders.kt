package app.simplecloud.npc.namespace.mythicmobs.option

import app.simplecloud.npc.namespace.mythicmobs.MobIdFetcher
import app.simplecloud.npc.shared.option.DefaultOptions
import app.simplecloud.npc.shared.option.Option
import app.simplecloud.npc.shared.option.OptionProvider
import io.lumine.mythic.core.mobs.ActiveMob

/**
 * @author Niklas Nieberler
 */

object MythicMobsOptionProviders {

    /**
     * Creates a [OptionProvider] for the interacting usage of npcs
     * @param mob the active mob
     */
    fun createInteractOptionProviders(mob: ActiveMob) = OptionProvider.with(
        Option.of(DefaultOptions.NPC_ID, MobIdFetcher.fetch(mob)),
        Option.of(DefaultOptions.NPC_NAME, mob.name),
        Option.of(DefaultOptions.NPC_UUID, mob.uniqueId),
        Option.of(DefaultOptions.NPC_ENTITY_ID, mob.entity.entityId),
    )

}