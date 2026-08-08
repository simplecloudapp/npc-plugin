package app.simplecloud.npc.plugin.paper.command

import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.shared.bridge.TargetResolver
import app.simplecloud.npc.shared.cloud.CloudService
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.provider.NpcProviderType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.future.await
import org.bukkit.Bukkit
import org.bukkit.Registry
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.annotations.suggestion.Suggestions

open class CommandSuggestions(
    protected val namespace: NpcNamespace,
) {
    @Suggestions("npcIds")
    fun suggestNpcId(): List<String> = namespace.npcRepository.selectors()

    @Suggestions("targets")
    fun suggestTargets(): List<String> = runBlocking { TargetResolver.suggestions() }

    @Suggestions("providers")
    fun suggestProviders(): List<String> {
        return namespace.providerRegistry.availableProviders(Bukkit.getPluginManager())
            .map { it.type.commandName }
            .sorted()
    }

    @Suggestions("creationProviders")
    fun suggestCreationProviders(): List<String> {
        return namespace.providerRegistry.availableProviders(Bukkit.getPluginManager())
            .filter { it.supportsCreation }
            .map { it.type.commandName }
            .sorted()
    }

    @Suggestions("providerReferences")
    fun suggestProviderReferences(context: CommandContext<CommandSourceStack>): List<String> {
        val providerName = context.optional<String>("provider").orElse(null) ?: return emptyList()
        val providerType = NpcProviderType.getOrNull(providerName) ?: return emptyList()
        val provider = namespace.providerRegistry.getAvailable(providerType, Bukkit.getPluginManager())
            ?: return emptyList()
        return provider.references()
            .filter { namespace.npcRepository.findByProvider(providerType, it) == null }
            .sorted()
    }

    @Suggestions("configuredTargets")
    fun suggestConfiguredTargets(context: CommandContext<CommandSourceStack>): List<String> {
        val selector = context.optional<String>("id").orElse(null) ?: return emptyList()
        return namespace.npcRepository.findBySelector(selector)
            ?.targetServers
            ?.sortedWith(String.CASE_INSENSITIVE_ORDER)
            .orEmpty()
    }

    @Suggestions("servers")
    fun suggestServers(): List<String> = runBlocking {
        buildList {
            addAll(CloudService.cloudApi.server().allServers.await().map { "${it.group.name}-${it.numericalId}" })
            addAll(CloudService.cloudApi.persistentServer().allPersistentServers.await().map { it.name })
        }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    @Suggestions("joinStates")
    fun suggestJoinStates(context: CommandContext<CommandSourceStack>): List<String> {
        val selector = context.optional<String>("id").orElse(null)
        val config = selector?.let(namespace.npcRepository::findBySelector)
        return buildList {
            add("default")
            add("maintenance")
            config?.hologram?.layouts?.mapTo(this) { it.joinState.lowercase() }
            config?.actions?.mapTo(this) { it.joinState.lowercase() }
        }.distinct().sorted()
    }

    @Suggestions("helpTopics")
    fun suggestHelpTopics(): List<String> = CommandMessages.helpTopics

    @Suggestions("playerInteractions")
    fun suggestPlayerInteraction(): List<String> = namespace.getAvailablePlayerInteractions().map { it.name.lowercase() }

    @Suggestions("actionTypes")
    fun suggestActionTypes(): List<String> = listOf(
        "join-target",
        "play-sound",
        "execute-command",
        "send-message",
        "teleport",
        "send-title",
        "send-to-server",
        "transfer-to-server",
    )

    @Suggestions("sounds")
    fun suggestSounds(): List<String> = Registry.SOUNDS.mapNotNull { Registry.SOUNDS.getKey(it)?.asString() }.sorted()
}
