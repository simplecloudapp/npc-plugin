package app.simplecloud.npc.bukkit.skin

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.render.NpcRenderer
import java.util.UUID

interface FixedSkinRenderer : NpcRenderer {
    override val supportsFixedSkin: Boolean get() = true

    override fun captureSkin(player: NpcPlayer): NpcConfig.SkinConfiguration = PlayerSkins.capture(player)

    override suspend fun captureSkinByUsername(username: String): NpcConfig.SkinConfiguration? =
        PlayerSkins.byUsername(username)

    override suspend fun captureSkinByUuid(uuid: UUID): NpcConfig.SkinConfiguration? = PlayerSkins.byUuid(uuid)
}
