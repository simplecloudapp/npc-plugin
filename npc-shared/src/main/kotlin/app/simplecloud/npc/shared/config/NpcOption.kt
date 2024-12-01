package app.simplecloud.npc.shared.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

/**
 * @author Niklas Nieberler
 */

@ConfigSerializable
data class NpcOption(
    val key: String = "",
    val value: String = ""
)