package app.simplecloud.npc.bukkit.glow

import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.GlowColors
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object GlowTeams : Listener {

    private class Member(val scoreboardEntry: String, val glowColor: String)

    private val members = ConcurrentHashMap<UUID, Member>()
    private val created = GlowTeamPackets.CreationTracker()
    private val warnedEntries = ConcurrentHashMap.newKeySet<String>()
    private val logger = NpcLog.logger

    fun install(plugin: Plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun shutdown() {
        HandlerList.unregisterAll(this)
        created.removeAll()
        members.clear()
        warnedEntries.clear()
    }

    private fun scoreboardEntryOf(entity: Entity): String =
        (entity as? Player)?.name ?: entity.uniqueId.toString()

    fun set(entity: Entity, glowColor: String?) {
        val entry = scoreboardEntryOf(entity)
        if (Bukkit.getOnlinePlayers().any { it.name == entry && it.uniqueId != entity.uniqueId }) {
            if (warnedEntries.add(entry)) {
                logger.warning(
                    "Not applying a glow color to the NPC named '$entry': a player with that name is online, " +
                        "and they would glow instead. Give the NPC a different name.",
                )
            }
            clear(entity.uniqueId)
            return
        }

        warnedEntries.remove(entry)
        set(entity.uniqueId, entry, glowColor)
    }

    private fun set(entityId: UUID, scoreboardEntry: String, glowColor: String?) {
        val color = glowColor?.lowercase()?.takeIf { GlowColors.resolve(it) != null }
        val previous =
            if (color == null) members.remove(entityId) else members.put(entityId, Member(scoreboardEntry, color))
        if (previous?.glowColor == color && previous?.scoreboardEntry == scoreboardEntry) return

        val next = color?.let { Member(scoreboardEntry, it) }
        Bukkit.getOnlinePlayers().forEach { player ->
            previous?.let { remove(player, it) }
            next?.let { add(player, it) }
        }
    }

    fun clear(entityId: UUID) {
        val previous = members.remove(entityId) ?: return
        Bukkit.getOnlinePlayers().forEach { remove(it, previous) }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        members.values.forEach { add(event.player, it) }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        created.forget(event.player.uniqueId)
    }

    private fun add(player: Player, member: Member) {
        val teamName = GlowTeamPackets.outlineTeamFor(member.glowColor) ?: return
        created.ensure(player, teamName, GlowColors.resolve(member.glowColor), hideNameTag = false)
        GlowTeamPackets.send(player, GlowTeamPackets.addEntry(teamName, member.scoreboardEntry))
    }

    private fun remove(player: Player, member: Member) {
        val teamName = GlowTeamPackets.outlineTeamFor(member.glowColor) ?: return
        GlowTeamPackets.send(player, GlowTeamPackets.removeEntry(teamName, member.scoreboardEntry))
    }
}
