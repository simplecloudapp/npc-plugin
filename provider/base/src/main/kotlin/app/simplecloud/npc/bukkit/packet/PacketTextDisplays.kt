package app.simplecloud.npc.bukkit.packet

import app.simplecloud.npc.core.hologram.HologramAlignment
import app.simplecloud.npc.core.hologram.HologramLine
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import me.tofaa.entitylib.meta.EntityMeta
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta
import me.tofaa.entitylib.meta.display.TextDisplayMeta
import org.bukkit.entity.Player
import java.util.Optional
import java.util.UUID
import app.simplecloud.npc.relocate.packets.kyori.adventure.text.Component as PacketComponent

object PacketTextDisplays {

    private val TEXT_INDEX = TextDisplayMeta.OFFSET.toInt()

    fun spawn(entityId: Int, uuid: UUID, x: Double, y: Double, z: Double): PacketWrapper<*> =
        WrapperPlayServerSpawnEntity(
            entityId,
            Optional.of(uuid),
            EntityTypes.TEXT_DISPLAY,
            Vector3d(x, y, z),
            0f,
            0f,
            0f,
            0,
            Optional.empty(),
        )

    fun metadata(entityId: Int, style: HologramLine, text: PacketComponent): PacketWrapper<*> =
        WrapperPlayServerEntityMetadata(entityId, entries(entityId, style, text))

    fun text(entityId: Int, text: PacketComponent): PacketWrapper<*> =
        WrapperPlayServerEntityMetadata(entityId, listOf(EntityData(TEXT_INDEX, EntityDataTypes.ADV_COMPONENT, text)))

    fun destroy(entityIds: Collection<Int>): PacketWrapper<*> =
        WrapperPlayServerDestroyEntities(*entityIds.toIntArray())

    fun send(player: Player, packets: List<PacketWrapper<*>>) {
        if (packets.isEmpty()) return
        val manager = PacketEvents.getAPI().playerManager
        if (packets.size == 1) return manager.sendPacket(player, packets.single())

        manager.sendPacket(player, WrapperPlayServerBundle())
        packets.forEach { manager.sendPacket(player, it) }
        manager.sendPacket(player, WrapperPlayServerBundle())
    }

    fun entries(entityId: Int, style: HologramLine, text: PacketComponent): List<EntityData<*>> {
        val meta = EntityMeta.createMeta(entityId, EntityTypes.TEXT_DISPLAY) as TextDisplayMeta
        meta.setText(text)
        meta.billboardConstraints = AbstractDisplayMeta.BillboardConstraints.valueOf(style.billboard.name)
        meta.isAlignLeft = style.alignment == HologramAlignment.LEFT
        meta.isAlignRight = style.alignment == HologramAlignment.RIGHT
        style.shadow?.let { meta.isShadow = it }
        style.lineWidth?.let { meta.lineWidth = it }
        style.viewRange?.let { meta.viewRange = it }
        style.shadowRadius?.let { meta.shadowRadius = it }
        style.displayHeight?.let { meta.height = it }
        style.displayWidth?.let { meta.width = it }

        return meta.createPacket().entityMetadata
    }
}
