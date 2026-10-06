package app.simplecloud.npc.paper.provider

import app.simplecloud.npc.bukkit.interact.OnNpcInteract
import app.simplecloud.npc.core.render.NpcRenderer
import org.bukkit.plugin.Plugin

class ProviderDescriptor(
    val name: String,
    val requiredPlugin: String? = null,
    val gate: () -> Boolean = { true },
    val create: (plugin: Plugin, onInteract: OnNpcInteract) -> NpcRenderer?,
)
