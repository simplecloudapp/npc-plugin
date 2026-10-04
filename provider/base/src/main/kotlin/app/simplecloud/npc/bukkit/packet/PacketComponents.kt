package app.simplecloud.npc.bukkit.packet

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import app.simplecloud.npc.relocate.packets.kyori.adventure.text.Component as PacketComponent
import app.simplecloud.npc.relocate.packets.kyori.adventure.text.format.NamedTextColor as PacketNamedTextColor
import app.simplecloud.npc.relocate.packets.kyori.adventure.text.serializer.gson.GsonComponentSerializer as PacketGsonSerializer

object PacketComponents {

    fun of(component: Component): PacketComponent =
        if (component == Component.empty()) {
            PacketComponent.empty()
        } else {
            PacketGsonSerializer.gson().deserialize(GsonComponentSerializer.gson().serialize(component))
        }

    fun of(color: NamedTextColor): PacketNamedTextColor =
        requireNotNull(PacketNamedTextColor.NAMES.value(NamedTextColor.NAMES.keyOrThrow(color))) {
            "no packet colour for $color"
        }
}
