package app.simplecloud.npc.bukkit.skin

import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.platform.NpcPlayer
import com.destroystokyo.paper.profile.PlayerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import java.util.UUID
import java.util.logging.Level

object PlayerSkins {

    fun capture(player: NpcPlayer): NpcConfig.SkinConfiguration =
        Bukkit.getPlayer(player.uniqueId)?.let { skinOf(it.playerProfile, player.name) }
            ?: NpcConfig.SkinConfiguration(sourcePlayer = player.name)

    suspend fun byUsername(username: String): NpcConfig.SkinConfiguration? =
        lookUp(Bukkit.createProfile(username), username)

    suspend fun byUuid(uuid: UUID): NpcConfig.SkinConfiguration? = lookUp(Bukkit.createProfile(uuid), null)

    private suspend fun lookUp(profile: PlayerProfile, requestedName: String?): NpcConfig.SkinConfiguration? =
        withContext(Dispatchers.IO) {
            runCatching { if (profile.complete(true, true)) skinOf(profile, requestedName) else null }
                .onFailure {
                    NpcLog.logger.log(Level.WARNING, "Skin lookup failed for ${requestedName ?: profile}", it)
                }
                .getOrNull()
        }

    private fun skinOf(profile: PlayerProfile, requestedName: String?): NpcConfig.SkinConfiguration? =
        profile.properties.firstOrNull { it.name == "textures" }?.let {
            NpcConfig.SkinConfiguration(it.value, it.signature, requestedName ?: profile.name)
        }
}
