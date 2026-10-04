package app.simplecloud.npc.paper.editor

import app.simplecloud.npc.common.editor.ChatInputPrompts
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class EditorChatListener(
    private val prompts: ChatInputPrompts,
    private val plugin: Plugin,
) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(event: AsyncChatEvent) {
        if (!prompts.isPending(event.player.uniqueId)) return
        event.isCancelled = true
        if (!plugin.isEnabled) return
        val text = PlainTextComponentSerializer.plainText().serialize(event.originalMessage())
        val uuid = event.player.uniqueId

        Bukkit.getScheduler().runTask(
            plugin,
            Runnable { prompts.submit(uuid, text) },
        )
    }
}
