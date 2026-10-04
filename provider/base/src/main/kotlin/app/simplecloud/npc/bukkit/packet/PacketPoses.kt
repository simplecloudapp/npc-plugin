package app.simplecloud.npc.bukkit.packet

import app.simplecloud.npc.core.config.NpcPose
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose

object PacketPoses {
    fun of(pose: NpcPose): EntityPose = when (pose) {
        NpcPose.STANDING -> EntityPose.STANDING
        NpcPose.SNEAKING -> EntityPose.CROUCHING
        NpcPose.SWIMMING -> EntityPose.SWIMMING
        NpcPose.SLEEPING -> EntityPose.SLEEPING
    }
}
