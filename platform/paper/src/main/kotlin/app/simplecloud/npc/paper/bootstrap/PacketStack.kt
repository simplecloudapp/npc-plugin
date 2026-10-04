package app.simplecloud.npc.paper.bootstrap

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import me.tofaa.entitylib.APIConfig
import me.tofaa.entitylib.EntityLib
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

object PacketStack {

    fun load(plugin: JavaPlugin) {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin))
        PacketEvents.getAPI().apply {
            settings.checkForUpdates(false)
            load()
        }
    }

    fun start(plugin: JavaPlugin) {
        PacketEvents.getAPI().init()
        val platform = SpigotEntityLibPlatform(plugin)
        EntityLib.init(platform, APIConfig(PacketEvents.getAPI()).usePlatformLogger())
        platform.setEntityIdProvider { _, _ -> Bukkit.getUnsafe().nextEntityId() }
    }

    fun shutdown() {
        val api = PacketEvents.getAPI() ?: return
        when {
            api.isInitialized -> api.terminate()
            api.isLoaded && !api.isTerminated -> api.injector.uninject()
        }
    }
}
