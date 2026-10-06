package app.simplecloud.npc.common.command

import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.core.cloud.CloudListCache
import app.simplecloud.npc.core.cloud.CloudNames
import app.simplecloud.npc.core.cloud.JoinStates
import app.simplecloud.npc.core.config.ActionFields
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.platform.NpcCommandSender
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import kotlin.jvm.optionals.getOrNull

class CommandSuggestions<C : NpcCommandSender>(
    private val pluginContext: NpcPluginContext,
) {
    @Suggestions("npcIds")
    fun suggestNpcId(): List<String> = pluginContext.npcRepository.ids()

    @Suggestions("targets")
    fun suggestTargets(): List<String> = CloudNames.targets()

    @Suggestions("configuredTargets")
    fun suggestConfiguredTargets(context: CommandContext<C>): List<String> =
        context.optional<String>("id").getOrNull()
            ?.let(pluginContext.npcRepository::find)
            ?.targetServers
            ?.sortedWith(String.CASE_INSENSITIVE_ORDER)
            .orEmpty()

    @Suggestions("linkProviders")
    fun suggestLinkProviders(): List<String> = pluginContext.providers.linkingProviders.sorted()

    @Suggestions("linkReferences")
    fun suggestLinkReferences(context: CommandContext<C>): List<String> {
        val provider = context.optional<String>("provider").getOrNull() ?: return emptyList()
        return runCatching { pluginContext.providers.linkableReferences(provider) }.getOrDefault(emptyList())
    }

    @Suggestions("servers")
    fun suggestServers(): List<String> = CloudNames.servers()

    @Suggestions("joinStates")
    fun suggestJoinStates(context: CommandContext<C>): List<String> {
        val selector = context.optional<String>("id").getOrNull() ?: return emptyList()
        val config = pluginContext.npcRepository.find(selector) ?: return emptyList()
        val live = JoinStates.live(config, CloudListCache.peek())

        return JoinStates.offered(config, live).sorted()
    }

    @Suggestions("helpTopics")
    fun suggestHelpTopics(): List<String> = CommandMessages.helpTopics

    @Suggestions("placeholderTopics")
    fun suggestPlaceholderTopics(): List<String> = PlaceholderPage.topics

    @Suggestions("playerInteractions")
    fun suggestPlayerInteraction(): List<String> = PlayerInteraction.entries.map { it.name.lowercase() }

    @Suggestions("actionTypes")
    fun suggestActionTypes(): List<String> = ActionFields.NAMES

    @Suggestions("onlinePlayers")
    fun suggestOnlinePlayers(): List<String> = pluginContext.playerDirectory.onlinePlayers().map { it.name }

    @Suggestions("inventoryIds")
    fun suggestInventoryIds(): List<String> = pluginContext.inventoryRepository.ids()
}
