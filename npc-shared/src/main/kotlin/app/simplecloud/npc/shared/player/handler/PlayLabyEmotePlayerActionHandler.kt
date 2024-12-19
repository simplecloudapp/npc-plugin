package app.simplecloud.npc.shared.player.handler

import app.simplecloud.npc.shared.enums.LabyEmote
import app.simplecloud.npc.shared.option.DefaultOptions
import app.simplecloud.npc.shared.option.OptionProvider
import app.simplecloud.npc.shared.player.PlayerActionHandler
import app.simplecloud.npc.shared.player.PlayerActionOptions
import net.labymod.serverapi.core.model.feature.Emote
import net.labymod.serverapi.core.packet.clientbound.game.feature.EmotePacket
import net.labymod.serverapi.server.bukkit.LabyModProtocolService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * @author Niklas Nieberler
 */

class PlayLabyEmotePlayerActionHandler : PlayerActionHandler {

    override fun handle(player: Player, optionProvider: OptionProvider) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LabyModServerAPI")) {
            Bukkit.getLogger().warning("SimpleCloud-NPC] To use this feature, you need to download the LabyModServerApi. Download it here: https://github.com/LabyMod/labymod4-server-api/releases")
            return
        }
        val entityId = optionProvider.getOption(DefaultOptions.NPC_UUID)
        val emoteName = optionProvider.getOption(PlayerActionOptions.PLAY_LABY_EMOTE)
        val labyEmote = LabyEmote.get(emoteName) ?: throw NullPointerException("failed to find emote id")
        val emote = Emote.play(UUID.fromString(entityId), labyEmote.key)
        println("npcId: $entityId | emote: $emote")
        LabyModProtocolService.get().labyModProtocol().sendPacket(player.uniqueId, EmotePacket(emote))
    }

    override fun getSuggestions() = mapOf(
        "play.emote" to LabyEmote.entries.map { it.displayName.lowercase().replace(" ", "_") }
    )

    private fun getUniqueIdByEntityId(player: Player, entityId: Int): UUID? {
        println(entityId)
        return player.world.entities.firstOrNull { it.entityId == entityId }?.uniqueId
    }

    override fun getOptions() = listOf(
        PlayerActionOptions.PLAY_LABY_EMOTE
    )
}