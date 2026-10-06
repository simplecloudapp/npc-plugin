package app.simplecloud.npc.core.render

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation

data class ProviderNpcSnapshot(
    val location: NpcLocation?,
    val skin: NpcConfig.SkinConfiguration? = null,
    val customName: String? = null,
)
