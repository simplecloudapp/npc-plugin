package app.simplecloud.npc.paper.effects

import app.simplecloud.npc.bukkit.compat.EntityIds
import app.simplecloud.npc.bukkit.hologram.HologramGeometry
import app.simplecloud.npc.bukkit.interact.InteractableEntities
import app.simplecloud.npc.bukkit.packet.PacketComponents
import app.simplecloud.npc.bukkit.packet.PacketPoses
import app.simplecloud.npc.bukkit.packet.PacketTextDisplays
import app.simplecloud.npc.common.platform.NpcEffects
import app.simplecloud.npc.core.config.NpcAnimation
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.platform.NpcPlayer
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PaperNpcEffects(private val plugin: Plugin) : NpcEffects {

    private val bubbles = ConcurrentHashMap<Pair<UUID, String>, Int>()

    override fun animate(npc: NpcConfig, player: NpcPlayer, animation: NpcAnimation) {
        val viewer = Bukkit.getPlayer(player.uniqueId) ?: return
        val entityIds = InteractableEntities.bodiesOf(npc.id)
        when (animation) {
            NpcAnimation.SWING_MAIN_HAND -> swing(viewer, entityIds, EntityAnimationType.SWING_MAIN_ARM)
            NpcAnimation.SWING_OFF_HAND -> swing(viewer, entityIds, EntityAnimationType.SWING_OFF_HAND)
            NpcAnimation.CROUCH -> crouch(viewer, entityIds, PacketPoses.of(npc.entity.pose))
        }
    }

    override fun speak(npc: NpcConfig, player: NpcPlayer, text: Component) {
        Bukkit.getScheduler().runTask(plugin, Runnable { showBubble(npc, player.uniqueId, text) })
    }

    fun shutdown() {
        bubbles.forEach { (key, entityId) ->
            Bukkit.getPlayer(key.first)?.let { send(it, PacketTextDisplays.destroy(listOf(entityId))) }
        }
        bubbles.clear()
    }

    private fun swing(viewer: Player, entityIds: List<Int>, type: EntityAnimationType) {
        entityIds.forEach { send(viewer, WrapperPlayServerEntityAnimation(it, type)) }
    }

    private fun crouch(viewer: Player, entityIds: List<Int>, restingPose: EntityPose) {
        entityIds.forEach { send(viewer, pose(it, EntityPose.CROUCHING)) }
        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                if (viewer.isOnline) entityIds.forEach { send(viewer, pose(it, restingPose)) }
            },
            CROUCH_TICKS,
        )
    }

    private fun showBubble(npc: NpcConfig, playerId: UUID, text: Component) {
        val viewer = Bukkit.getPlayer(playerId) ?: return
        val location = npc.entity.location
        val world = Bukkit.getWorld(location.world) ?: return
        if (viewer.world != world) return

        val key = viewer.uniqueId to npc.id
        bubbles.remove(key)?.let { send(viewer, PacketTextDisplays.destroy(listOf(it))) }

        val entityId = EntityIds.next(world)
        val y = location.y + bubbleHeight(npc)
        PacketTextDisplays.send(
            viewer,
            listOf(
                PacketTextDisplays.spawn(entityId, UUID.randomUUID(), location.x, y, location.z),
                PacketTextDisplays.metadata(entityId, BUBBLE_STYLE, PacketComponents.of(text)),
            ),
        )
        bubbles[key] = entityId

        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                if (!bubbles.remove(key, entityId)) return@Runnable
                Bukkit.getPlayer(playerId)?.let { send(it, PacketTextDisplays.destroy(listOf(entityId))) }
            },
            BUBBLE_TICKS,
        )
    }

    private fun bubbleHeight(npc: NpcConfig): Double {
        val hologram = npc.hologram
        if (!hologram.enabled) return HEAD_HEIGHT * npc.entity.heightFactor() + GAP
        val lines = hologram.layouts.maxOfOrNull { it.lines.size } ?: 0

        return HologramGeometry.topHeight(npc, lines) + GAP
    }

    private fun pose(entityId: Int, pose: EntityPose) =
        WrapperPlayServerEntityMetadata(entityId, listOf(EntityData(POSE_INDEX, EntityDataTypes.ENTITY_POSE, pose)))

    private fun send(player: Player, packet: PacketWrapper<*>) {
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    private companion object {
        const val POSE_INDEX = 6
        const val CROUCH_TICKS = 8L
        const val BUBBLE_TICKS = 60L
        const val HEAD_HEIGHT = 2.0
        const val GAP = 0.35
        val BUBBLE_STYLE = HologramLine()
    }
}
