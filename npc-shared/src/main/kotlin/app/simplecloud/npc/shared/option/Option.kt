package app.simplecloud.npc.shared.option

import app.simplecloud.npc.shared.config.NpcConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Niklas Nieberler
 */

data class Option(
    val key: String = "",
    val value: Any = "",
) {

    companion object {
        fun of(npcOption: NpcConfig.NpcOption): Option {
            return Option(npcOption.key, npcOption.value)
        }

        fun <T> of(pair: Pair<String, Any>, value: T): Option {
            return Option(pair.first, value!!)
        }
    }

}