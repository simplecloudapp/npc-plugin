package app.simplecloud.npc.core.platform

import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import net.kyori.adventure.text.Component
import java.util.UUID

interface NpcPlayer : NpcCommandSender {
    val uniqueId: UUID
    val name: String

    fun location(): NpcLocation
    fun performCommand(command: String)
    fun playSound(key: String, options: NpcConfig.SoundOptions = NpcConfig.SoundOptions())
    fun showTitle(title: Component, subtitle: Component, fadeInTicks: Int, stayTicks: Int, fadeOutTicks: Int)
    fun teleport(location: NpcLocation): Boolean
    fun push(from: NpcLocation, strength: Double, vertical: Double)
    fun sendToServer(serverName: String)
    fun transferToServer(host: String, port: Int)
    fun closeInventory()
    fun wornArmor(): NpcConfig.EquipmentConfiguration
    fun sendActionBar(component: Component)

    override fun asPlayer(): NpcPlayer = this
}
