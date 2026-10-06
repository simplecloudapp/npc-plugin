package app.simplecloud.npc.bukkit.glow

import app.simplecloud.npc.bukkit.packet.PacketComponents
import app.simplecloud.npc.core.config.GlowColors
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.CollisionRule
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.NameTagVisibility
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import app.simplecloud.npc.relocate.packets.kyori.adventure.text.Component as PacketComponent

object GlowTeamPackets {

    private const val HIDDEN_NAME_TAG_TEAM = "scnpcs_hidden"

    private const val HIDDEN_NAME_TAG_GLOW_PREFIX = "scnpcs_glow_"
    private const val OUTLINE_PREFIX = "scnpcs_outline_"

    fun hiddenNameTagTeamFor(glowColor: String?): String =
        GlowColors.resolve(glowColor)?.let { HIDDEN_NAME_TAG_GLOW_PREFIX + glowColor!!.lowercase() }
            ?: HIDDEN_NAME_TAG_TEAM

    fun outlineTeamFor(glowColor: String?): String? =
        GlowColors.resolve(glowColor)?.let { OUTLINE_PREFIX + glowColor!!.lowercase() }

    fun create(teamName: String, color: NamedTextColor?, hideNameTag: Boolean): WrapperPlayServerTeams {
        val nameTagVisibility = if (hideNameTag) NameTagVisibility.NEVER else NameTagVisibility.ALWAYS
        val collisionRule = if (hideNameTag) CollisionRule.NEVER else CollisionRule.ALWAYS
        val info = WrapperPlayServerTeams.ScoreBoardTeamInfo(
            PacketComponent.empty(),
            PacketComponent.empty(),
            PacketComponent.empty(),
            nameTagVisibility,
            collisionRule,
            PacketComponents.of(color ?: NamedTextColor.WHITE),
            WrapperPlayServerTeams.OptionData.NONE,
        )

        return WrapperPlayServerTeams(teamName, WrapperPlayServerTeams.TeamMode.CREATE, info, emptyList())
    }

    fun addEntry(teamName: String, entry: String): WrapperPlayServerTeams =
        WrapperPlayServerTeams(
            teamName,
            WrapperPlayServerTeams.TeamMode.ADD_ENTITIES,
            null as WrapperPlayServerTeams.ScoreBoardTeamInfo?,
            listOf(entry),
        )

    fun removeEntry(teamName: String, entry: String): WrapperPlayServerTeams =
        WrapperPlayServerTeams(
            teamName,
            WrapperPlayServerTeams.TeamMode.REMOVE_ENTITIES,
            null as WrapperPlayServerTeams.ScoreBoardTeamInfo?,
            listOf(entry),
        )

    fun remove(teamName: String): WrapperPlayServerTeams =
        WrapperPlayServerTeams(
            teamName,
            WrapperPlayServerTeams.TeamMode.REMOVE,
            null as WrapperPlayServerTeams.ScoreBoardTeamInfo?,
            emptyList<String>(),
        )

    fun send(player: Player, packet: PacketWrapper<*>) {
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    class CreationTracker {
        private val created = ConcurrentHashMap<UUID, MutableSet<String>>()

        fun ensure(player: Player, teamName: String, color: NamedTextColor?, hideNameTag: Boolean) {
            val teams = created.getOrPut(player.uniqueId) { ConcurrentHashMap.newKeySet() }
            if (!teams.add(teamName)) return

            send(player, create(teamName, color, hideNameTag))
        }

        fun forget(playerId: UUID) {
            created.remove(playerId)
        }

        fun removeAll() {
            created.forEach { (playerId, teams) ->
                val player = Bukkit.getPlayer(playerId) ?: return@forEach
                teams.forEach { runCatching { send(player, remove(it)) } }
            }
            created.clear()
        }
    }
}
