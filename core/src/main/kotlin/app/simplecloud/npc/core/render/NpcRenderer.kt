package app.simplecloud.npc.core.render

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.core.platform.NpcPlayer
import java.util.UUID

interface NpcRenderer {

    fun onEnable()
    fun onDisable()

    fun isReady(): Boolean = true

    val supportsCreation: Boolean get() = true
    val supportsFixedSkin: Boolean get() = false
    val supportsLinking: Boolean get() = false

    fun linkableReferences(): List<String> = emptyList()
    fun spawn(config: NpcConfig): NpcConfig
    fun despawn(config: NpcConfig)
    fun refresh(config: NpcConfig)
    fun teleport(config: NpcConfig)
    fun locationOf(config: NpcConfig): NpcLocation?
    fun snapshotOf(config: NpcConfig): ProviderNpcSnapshot? = null
    fun canonicalReference(config: NpcConfig): String? = null
    fun captureSkin(player: NpcPlayer): NpcConfig.SkinConfiguration =
        NpcConfig.SkinConfiguration(sourcePlayer = player.name)

    suspend fun captureSkinByUsername(username: String): NpcConfig.SkinConfiguration? = null
    suspend fun captureSkinByUuid(uuid: UUID): NpcConfig.SkinConfiguration? = null
}
