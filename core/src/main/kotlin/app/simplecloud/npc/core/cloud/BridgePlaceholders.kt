package app.simplecloud.npc.core.cloud

import app.simplecloud.plugin.api.shared.placeholder.PlaceholderProvider
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object BridgePlaceholders {
    suspend fun append(bridge: ServerBridge, text: String, prefix: String = "target"): Component = when (bridge) {
        is ServerBridge.OfPersistentServer ->
            PlaceholderProvider.persistentServerPlaceholderProvider.append(bridge.persistentServer, text, prefix)

        is ServerBridge.OfGroup -> PlaceholderProvider.groupPlaceholderProvider.append(bridge.group, text, prefix)
        is ServerBridge.OfServer -> PlaceholderProvider.serverPlaceholderProvider.append(bridge.server, text, prefix)
    }

    suspend fun resolver(bridge: ServerBridge, prefix: String = "target"): TagResolver = when (bridge) {
        is ServerBridge.OfPersistentServer -> PlaceholderProvider.persistentServerPlaceholderProvider
            .getTagResolver(listOf(bridge.persistentServer), prefix)

        is ServerBridge.OfGroup -> PlaceholderProvider.groupPlaceholderProvider.getTagResolver(listOf(bridge.group), prefix)
        is ServerBridge.OfServer -> PlaceholderProvider.serverPlaceholderProvider.getTagResolver(listOf(bridge.server), prefix)
    }
}
