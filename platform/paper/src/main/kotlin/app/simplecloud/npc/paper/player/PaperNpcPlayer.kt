package app.simplecloud.npc.paper.player

import app.simplecloud.npc.bukkit.equipment.ArmorLooks
import app.simplecloud.npc.bukkit.scheduling.sync
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.paper.pushback.Pushback
import app.simplecloud.npc.paper.sound.SoundPlayer
import com.google.common.io.ByteStreams
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.util.Vector
import java.util.UUID

class PaperNpcPlayer(
    private val plugin: Plugin,
    private val player: Player,
) : NpcPlayer {

    override val uniqueId: UUID get() = player.uniqueId
    override val name: String get() = player.name

    override fun sendMessage(component: Component) =
        plugin.sync { player.sendMessage(component) }

    override fun location(): NpcLocation = plugin.sync {
        val location = player.location
        val world = location.world?.name ?: player.world.name

        NpcLocation(world, location.x, location.y, location.z, location.yaw, location.pitch)
    }

    override fun closeInventory() {
        plugin.sync { player.closeInventory() }
    }

    override fun hasPermission(permission: String): Boolean = plugin.sync { player.hasPermission(permission) }

    override fun wornArmor(): NpcConfig.EquipmentConfiguration = plugin.sync {
        val inventory = player.inventory
        fun look(stack: ItemStack?) = stack?.takeUnless { it.type.isAir }?.let(ArmorLooks::read)

        NpcConfig.EquipmentConfiguration(
            helmet = look(inventory.helmet),
            chestplate = look(inventory.chestplate),
            leggings = look(inventory.leggings),
            boots = look(inventory.boots),
        )
    }

    override fun sendActionBar(component: Component) {
        plugin.sync { player.sendActionBar(component) }
    }

    override fun performCommand(command: String) {
        plugin.sync { player.performCommand(command) }
    }

    override fun playSound(key: String, options: NpcConfig.SoundOptions) {
        plugin.sync { SoundPlayer.play(player, key, options) }
    }

    override fun showTitle(title: Component, subtitle: Component, fadeInTicks: Int, stayTicks: Int, fadeOutTicks: Int) {
        plugin.sync { TitleSender.show(player, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks) }
    }

    override fun teleport(location: NpcLocation): Boolean = plugin.sync {
        val world = Bukkit.getWorld(location.world) ?: return@sync false
        player.teleport(Location(world, location.x, location.y, location.z, location.yaw, location.pitch))
    }

    override fun push(from: NpcLocation, strength: Double, vertical: Double) {
        plugin.sync {
            if (!player.world.name.equals(from.world, true)) return@sync

            Pushback.push(
                player,
                player.location.toVector(),
                player.location.yaw,
                Vector(from.x, from.y, from.z),
                strength,
                vertical,
            )
        }
    }

    override fun sendToServer(serverName: String) {
        plugin.sync {
            val out = ByteStreams.newDataOutput().apply {
                writeUTF("Connect")
                writeUTF(serverName)
            }
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())
        }
    }

    override fun transferToServer(host: String, port: Int) {
        plugin.sync { player.transfer(host, port) }
    }
}
