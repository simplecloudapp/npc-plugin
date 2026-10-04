package app.simplecloud.npc.bukkit.compat

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.manager.server.ServerVersion

object ServerCompat {

    val version: ServerVersion by lazy { PacketEvents.getAPI().serverManager.version }

    private fun atLeast(minimum: ServerVersion): Boolean = version.isNewerThanOrEquals(minimum)

    val hasMannequin: Boolean get() = atLeast(ServerVersion.V_1_21_9)
}
