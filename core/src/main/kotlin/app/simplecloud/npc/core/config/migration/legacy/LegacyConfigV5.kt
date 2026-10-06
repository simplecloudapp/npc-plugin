package app.simplecloud.npc.core.config.migration.legacy

import app.simplecloud.npc.core.config.NpcConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Setting

@ConfigSerializable
data class LegacyNpcConfigV5(
    val id: String = "",
    val provider: LegacyProviderConfigurationV5 = LegacyProviderConfigurationV5(),
    @Setting("target-servers")
    val targetServers: MutableList<String> = mutableListOf(),
    val hologram: NpcConfig.HologramConfigurationRoot = NpcConfig.HologramConfigurationRoot(),
    val pushback: NpcConfig.PushbackConfiguration = NpcConfig.PushbackConfiguration(),
    val actions: MutableList<NpcConfig.ActionConfiguration> = mutableListOf(),
)

@ConfigSerializable
data class LegacyProviderConfigurationV5(
    val type: LegacyProviderType = LegacyProviderType.CITIZENS,
    val reference: String = "",
    val ownership: String = "LINKED",
)
