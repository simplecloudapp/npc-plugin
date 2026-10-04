package app.simplecloud.npc.paper.effects

import app.simplecloud.npc.bukkit.interact.InteractableEntities
import app.simplecloud.npc.bukkit.packet.PacketPoses
import app.simplecloud.npc.common.platform.NpcEffects
import app.simplecloud.npc.core.config.NpcAnimation
import app.simplecloud.npc.core.config.NpcConfig
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
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.Plugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PaperNpcEffects(private val plugin: Plugin) : NpcEffects {

    private val bubbles = ConcurrentHashMap<Pair<UUID, String>, UUID>()

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
        bubbles.values.forEach { Bukkit.getEntity(it)?.remove() }
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
        bubbles.remove(key)?.let { Bukkit.getEntity(it)?.remove() }

        val at = Location(world, location.x, location.y + bubbleHeight(npc), location.z)
        val bubble = world.spawn(at, TextDisplay::class.java) { display ->
            display.isVisibleByDefault = false
            display.isPersistent = false
            display.billboard = Display.Billboard.CENTER
            display.text(text)
        }
        viewer.showEntity(plugin, bubble)
        bubbles[key] = bubble.uniqueId

        Bukkit.getScheduler().runTaskLater(
            plugin,
            Runnable {
                if (viewer.isOnline) viewer.hideEntity(plugin, bubble)
                bubble.remove()
                bubbles.remove(key, bubble.uniqueId)
            },
            BUBBLE_TICKS,
        )
    }

    private fun bubbleHeight(npc: NpcConfig): Double {
        val factor = npc.entity.heightFactor()
        val hologram = npc.hologram
        if (!hologram.enabled) return HEAD_HEIGHT * factor + GAP
        val lines = hologram.layouts.maxOfOrNull { it.lines.size } ?: 0

        return hologram.startHeight * factor + LINE_SPACING * lines + GAP
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
        const val LINE_SPACING = 0.3
        const val GAP = 0.35
    }
}
