package app.simplecloud.npc.common

import app.simplecloud.npc.common.platform.NpcPlayerDirectory
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.render.HologramRenderer
import app.simplecloud.npc.core.render.NpcRenderer
import app.simplecloud.npc.core.render.ProviderNpcSnapshot
import net.kyori.adventure.text.Component
import java.util.UUID

class RecordingNpcRenderer(override val supportsCreation: Boolean = true) : NpcRenderer {
    val spawned = mutableListOf<String>()
    val despawned = mutableListOf<String>()
    val refreshed = mutableListOf<String>()

    override fun onEnable() = Unit
    override fun onDisable() = Unit
    override fun spawn(config: NpcConfig): NpcConfig = config.also { spawned += it.id }
    override fun despawn(config: NpcConfig) {
        despawned += config.id
    }

    override fun refresh(config: NpcConfig) {
        refreshed += config.id
    }

    override fun teleport(config: NpcConfig) = Unit

    override fun locationOf(config: NpcConfig): NpcLocation? = null
    override fun snapshotOf(config: NpcConfig): ProviderNpcSnapshot? = null
}

class RecordingHolograms : HologramRenderer {
    val updated = mutableListOf<String>()
    val destroyed = mutableListOf<String>()

    override fun createOrUpdate(config: NpcConfig) {
        updated += config.id
    }

    override fun destroy(config: NpcConfig) {
        destroyed += config.id
    }

    override fun destroyAll() = Unit
}

object NoHolograms : HologramRenderer {
    override fun createOrUpdate(config: NpcConfig) = Unit
    override fun destroy(config: NpcConfig) = Unit
    override fun destroyAll() = Unit
}

object NoPlayers : NpcPlayerDirectory {
    override fun findOnlinePlayer(name: String): NpcPlayer? = null
    override fun onlinePlayers(): List<NpcPlayer> = emptyList()
}

class FakePlayer(override val name: String = "Tester") : NpcPlayer {
    override val uniqueId: UUID = UUID.randomUUID()
    val messages = mutableListOf<Component>()
    var closedInventories = 0

    val calls = mutableListOf<String>()

    var soundFails = false

    override fun location(): NpcLocation = NpcLocation("world", 0.0, 64.0, 0.0, 0f, 0f)
    override fun performCommand(command: String) {
        calls += "command:$command"
    }

    override fun playSound(key: String, options: NpcConfig.SoundOptions) {
        calls += "sound:$key"
        if (soundFails) error("no such sound")
    }

    override fun showTitle(title: Component, subtitle: Component, fadeInTicks: Int, stayTicks: Int, fadeOutTicks: Int) {
        calls += "title"
    }

    override fun teleport(location: NpcLocation): Boolean {
        calls += "teleport"
        return true
    }

    override fun push(from: NpcLocation, strength: Double, vertical: Double) = Unit
    val permissions = mutableSetOf<String>()
    override fun wornArmor(): NpcConfig.EquipmentConfiguration = NpcConfig.EquipmentConfiguration()

    override fun hasPermission(permission: String): Boolean = permission in permissions

    override fun sendActionBar(component: Component) {
        calls += "actionbar"
    }

    override fun sendToServer(serverName: String) {
        calls += "send:$serverName"
    }

    override fun transferToServer(host: String, port: Int) {
        calls += "transfer:$host:$port"
    }
    override fun closeInventory() {
        closedInventories++
    }

    override fun sendMessage(component: Component) {
        calls += "message"
        messages += component
    }
}
