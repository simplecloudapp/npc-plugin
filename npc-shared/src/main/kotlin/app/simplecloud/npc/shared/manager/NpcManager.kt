package app.simplecloud.npc.shared.manager

import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.bridge.TargetResolution
import app.simplecloud.npc.shared.bridge.TargetResolver
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.hologram.config.HologramConfiguration
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.npc.shared.provider.NpcProvider
import app.simplecloud.npc.shared.provider.NpcProviderType
import app.simplecloud.npc.shared.provider.ProviderOwnership
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class NpcManager(
    private val namespace: NpcNamespace,
) {
    private val repository = namespace.npcRepository

    fun exist(id: String): Boolean = repository.find(id) != null

    suspend fun create(
        id: String,
        targetName: String,
        requestedProvider: NpcProviderType?,
        player: Player,
    ): NpcOperationResult {
        validateId(id)?.let { return it }
        if (exist(id)) return NpcOperationResult.Failure(NpcFailure.ALREADY_EXISTS, id)

        val target = when (val resolution = TargetResolver.resolve(targetName)) {
            is TargetResolution.Found -> resolution.target
            TargetResolution.Ambiguous -> return NpcOperationResult.Failure(NpcFailure.TARGET_AMBIGUOUS, targetName)
            TargetResolution.NotFound -> return NpcOperationResult.Failure(NpcFailure.TARGET_NOT_FOUND, targetName)
        }
        val creationProviders = namespace.providerRegistry.availableProviders(Bukkit.getPluginManager())
            .filter { it.supportsCreation }
        val provider = when {
            requestedProvider != null -> creationProviders.firstOrNull { it.type == requestedProvider }
                ?: return NpcOperationResult.Failure(NpcFailure.PROVIDER_UNAVAILABLE, requestedProvider.commandName)
            creationProviders.isEmpty() -> return NpcOperationResult.Failure(NpcFailure.CREATION_PROVIDER_UNAVAILABLE)
            creationProviders.size > 1 -> return NpcOperationResult.Failure(NpcFailure.PROVIDER_REQUIRED)
            else -> creationProviders.single()
        }

        var reference: String? = null
        return try {
            reference = provider.create(id, player, player.location.clone())
            val config = createDefaultConfig(id, target.name, provider, reference)
            repository.save(config)
            namespace.hologramManager.createOrUpdate(config)
            NpcOperationResult.Success(config)
        } catch (exception: Exception) {
            namespace.hologramManager.destroyHolograms(id)
            reference?.let { runCatching { provider.delete(it) } }
            repository.find(id)?.let { runCatching { repository.delete(it) } }
            NpcOperationResult.Failure(NpcFailure.CREATE_FAILED, exception.message)
        }
    }

    suspend fun link(
        id: String,
        targetName: String,
        providerType: NpcProviderType,
        reference: String,
    ): NpcOperationResult {
        validateId(id)?.let { return it }
        if (exist(id)) return NpcOperationResult.Failure(NpcFailure.ALREADY_EXISTS, id)

        val target = when (val resolution = TargetResolver.resolve(targetName)) {
            is TargetResolution.Found -> resolution.target
            TargetResolution.Ambiguous -> return NpcOperationResult.Failure(NpcFailure.TARGET_AMBIGUOUS, targetName)
            TargetResolution.NotFound -> return NpcOperationResult.Failure(NpcFailure.TARGET_NOT_FOUND, targetName)
        }
        val provider = namespace.providerRegistry.getAvailable(providerType, Bukkit.getPluginManager())
            ?: return NpcOperationResult.Failure(NpcFailure.PROVIDER_UNAVAILABLE, providerType.commandName)
        if (!provider.exists(reference)) {
            return NpcOperationResult.Failure(NpcFailure.PROVIDER_NPC_NOT_FOUND, reference)
        }
        if (repository.findByProvider(providerType, reference) != null) {
            return NpcOperationResult.Failure(NpcFailure.PROVIDER_NPC_ALREADY_LINKED, reference)
        }

        return try {
            val config = createDefaultConfig(id, target.name, provider, reference, ProviderOwnership.LINKED)
            repository.save(config)
            namespace.hologramManager.createOrUpdate(config)
            NpcOperationResult.Success(config)
        } catch (exception: Exception) {
            namespace.hologramManager.destroyHolograms(id)
            repository.find(id)?.let { runCatching { repository.delete(it) } }
            NpcOperationResult.Failure(NpcFailure.CREATE_FAILED, exception.message)
        }
    }

    fun unlink(id: String): NpcOperationResult {
        val config = repository.find(id) ?: return NpcOperationResult.Failure(NpcFailure.NOT_FOUND, id)
        namespace.hologramManager.destroyHolograms(id)
        repository.delete(config)
        return NpcOperationResult.Success(config)
    }

    fun delete(id: String): NpcOperationResult {
        val config = repository.find(id) ?: return NpcOperationResult.Failure(NpcFailure.NOT_FOUND, id)
        val provider = namespace.providerRegistry.getAvailable(config.provider.type, Bukkit.getPluginManager())
        if (config.provider.ownership == ProviderOwnership.MANAGED) {
            if (provider == null) {
                return NpcOperationResult.Failure(NpcFailure.PROVIDER_UNAVAILABLE, config.provider.type.commandName)
            }
            if (provider.exists(config.provider.reference) && !provider.delete(config.provider.reference)) {
                return NpcOperationResult.Failure(NpcFailure.DELETE_FAILED, id)
            }
        }
        namespace.hologramManager.destroyHolograms(id)
        repository.delete(config)
        return NpcOperationResult.Success(config)
    }

    private fun validateId(id: String): NpcOperationResult.Failure? {
        return if (ID_PATTERN.matches(id)) null else NpcOperationResult.Failure(NpcFailure.INVALID_ID, id)
    }

    private fun createDefaultConfig(
        id: String,
        target: String,
        provider: NpcProvider,
        reference: String,
        ownership: ProviderOwnership = ProviderOwnership.MANAGED,
    ): NpcConfig {
        return NpcConfig(
            id = id,
            provider = NpcConfig.ProviderConfiguration(provider.type, reference, ownership),
            targetServers = mutableListOf(target),
            hologram = NpcConfig.HologramConfigurationRoot(
                layouts = mutableListOf(
                    NpcConfig.HologramLayout(
                        joinState = "default",
                        lines = mutableListOf(
                            HologramConfiguration("<#0ea5e9><bold><target_name>"),
                            HologramConfiguration("<#94a3b8><target_online_players>/<target_max_players> online"),
                        ),
                    ),
                    NpcConfig.HologramLayout(
                        joinState = "maintenance",
                        lines = mutableListOf(
                            HologramConfiguration("<#dc2626><bold><target_name>"),
                            HologramConfiguration("<#94a3b8>Currently under maintenance"),
                        ),
                    ),
                )
            ),
            actions = mutableListOf(
                NpcConfig.ActionConfiguration(
                    interactionType = PlayerInteraction.RIGHT_CLICK,
                    joinState = "default",
                    joinTarget = true,
                ),
                NpcConfig.ActionConfiguration(
                    interactionType = PlayerInteraction.RIGHT_CLICK,
                    joinState = "maintenance",
                    sendMessage = "<#dc2626>This server is currently under maintenance.",
                ),
            ),
        )
    }

    companion object {
        private val ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}

sealed interface NpcOperationResult {
    data class Success(val config: NpcConfig) : NpcOperationResult
    data class Failure(val failure: NpcFailure, val detail: String? = null) : NpcOperationResult
}

enum class NpcFailure {
    INVALID_ID,
    ALREADY_EXISTS,
    NOT_FOUND,
    TARGET_NOT_FOUND,
    TARGET_AMBIGUOUS,
    PROVIDER_REQUIRED,
    CREATION_PROVIDER_UNAVAILABLE,
    PROVIDER_UNAVAILABLE,
    PROVIDER_NPC_NOT_FOUND,
    PROVIDER_NPC_ALREADY_LINKED,
    CREATE_FAILED,
    DELETE_FAILED,
}
