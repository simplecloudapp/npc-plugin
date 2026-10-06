package app.simplecloud.npc.paper.bootstrap

import app.simplecloud.npc.bukkit.compat.EntityIds
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.PacketEventsAPI
import com.github.retrooper.packetevents.util.logger.JulLegacyLogManager
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import me.tofaa.entitylib.APIConfig
import me.tofaa.entitylib.EntityLib
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

object PacketStack {

    fun load(plugin: JavaPlugin) {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(plugin).also(::quiet))
        PacketEvents.getAPI().apply {
            settings.checkForUpdates(false)
            load()
        }
    }

    fun start(plugin: JavaPlugin) {
        PacketEvents.getAPI().init()
        val platform = SpigotEntityLibPlatform(plugin)
        EntityLib.init(platform, APIConfig(PacketEvents.getAPI()).usePlatformLogger())
        platform.setEntityIdProvider { _, _ -> EntityIds.next() }
    }

    private fun quiet(api: PacketEventsAPI<*>) {
        runCatching {
            val field = api.javaClass.getDeclaredField("logManager").apply { isAccessible = true }
            field.set(api, JulLegacyLogManager(api))
            JulLegacyLogManager.getLogger().level = Level.WARNING
        }
    }

    fun shutdown() {
        val api = PacketEvents.getAPI() ?: return
        when {
            api.isInitialized -> api.terminate()
            api.isLoaded && !api.isTerminated -> api.injector.uninject()
        }
    }
}
