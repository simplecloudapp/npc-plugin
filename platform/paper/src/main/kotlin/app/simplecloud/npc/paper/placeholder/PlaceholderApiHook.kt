package app.simplecloud.npc.paper.placeholder

import app.simplecloud.npc.bukkit.scheduling.sync
import app.simplecloud.npc.core.text.PlayerPlaceholders
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.plugin.Plugin
import java.util.UUID

object PlaceholderApiHook {

    fun install(plugin: Plugin) {
        if (plugin.server.pluginManager.getPlugin("PlaceholderAPI")?.isEnabled != true) return
        val setPlaceholders = runCatching {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                .getMethod("setPlaceholders", OfflinePlayer::class.java, String::class.java)
        }.getOrElse {
            plugin.logger.warning("PlaceholderAPI is installed but could not be hooked: ${it.message}")
            return
        }

        PlayerPlaceholders.external = { uuid: UUID, text: String ->
            plugin.sync { setPlaceholders.invoke(null, Bukkit.getOfflinePlayer(uuid), text) as? String ?: text }
        }
        plugin.logger.info("Hooked into PlaceholderAPI.")
    }

    fun uninstall() {
        PlayerPlaceholders.external = null
    }
}
