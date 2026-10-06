package app.simplecloud.npc.common.manager

import app.simplecloud.npc.common.render.ProviderCapabilities
import app.simplecloud.npc.common.text.Msg
import app.simplecloud.npc.common.utils.InputChecks
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.cloud.CloudSnapshot
import app.simplecloud.npc.core.cloud.JoinStateResolver
import app.simplecloud.npc.core.cloud.ResolvedTarget
import app.simplecloud.npc.core.cloud.TargetResolution
import app.simplecloud.npc.core.cloud.TargetResolver
import app.simplecloud.npc.core.config.ConfigIds
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.hologram.HologramLine
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.render.HologramRenderer
import app.simplecloud.npc.core.render.NpcRenderer
import app.simplecloud.npc.core.render.Providers
import app.simplecloud.npc.core.repository.NpcRepository
import app.simplecloud.npc.core.repository.ReloadDiff
import kotlinx.coroutines.CancellationException
import java.util.logging.Level
import kotlin.math.abs

class NpcManager(
    private val repository: NpcRepository,
    private val renderer: NpcRenderer,
    private val hologramRenderer: HologramRenderer,
    private val providers: ProviderCapabilities,
) {
    fun save(config: NpcConfig, refreshHologram: Boolean = true) {
        repository.save(config)
        if (refreshHologram) hologramRenderer.createOrUpdate(config)
    }

    fun reload(): ReloadDiff<NpcConfig> = repository.reload().also(::apply)

    fun apply(diff: ReloadDiff<NpcConfig>) {
        diff.removed.forEach { config ->
            perNpc(config.id, "remove") {
                hologramRenderer.destroy(config)
                renderer.despawn(config)
            }
        }
        diff.added.forEach { config -> perNpc(config.id, "place") { spawnAndSave(config) } }
        diff.changed.forEach { change ->
            perNpc(change.current.id, "update") {
                if (change.previous.entity.provider != change.current.entity.provider) {
                    hologramRenderer.destroy(change.previous)
                    renderer.despawn(change.previous)
                    spawnAndSave(change.current)
                } else {
                    renderer.refresh(change.current)
                    hologramRenderer.createOrUpdate(change.current)
                }
            }
        }
    }

    fun reconcileOnBoot(): List<String> {
        val reconciled = repository.findAll().map { original ->
            perNpc(original.id, "place", fallback = original) {
                var config = reconcileProvider(original)
                if (config.entity.needsRelocation) {
                    if (config != original) repository.save(config)
                    return@perNpc config
                }

                config = withCanonicalReference(renderer.spawn(config))
                renderer.locationOf(config)
                    ?.takeUnless { it.isSameSpot(config.entity.location) }
                    ?.let { live -> config = config.copy(entity = config.entity.copy(location = live)) }

                if (config != original) repository.save(config)
                hologramRenderer.createOrUpdate(config)
                config
            }
        }

        return reconciled.filter { it.entity.needsRelocation }.map { it.id }
    }

    private fun perNpc(id: String, action: String, block: () -> Unit) = perNpc(id, action, Unit, block)

    private fun <T> perNpc(id: String, action: String, fallback: T, block: () -> T): T = try {
        block()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        logger.log(Level.WARNING, "Could not $action NPC '$id'", exception)
        fallback
    }

    private fun NpcLocation.isSameSpot(other: NpcLocation): Boolean =
        world.equals(other.world, ignoreCase = true) &&
            abs(x - other.x) <= LOCATION_DRIFT_TOLERANCE &&
            abs(y - other.y) <= LOCATION_DRIFT_TOLERANCE &&
            abs(z - other.z) <= LOCATION_DRIFT_TOLERANCE

    private fun spawnAndSave(config: NpcConfig) {
        val spawned = withCanonicalReference(renderer.spawn(config))
        if (spawned != config) repository.save(spawned)
        hologramRenderer.createOrUpdate(spawned)
    }

    private fun withCanonicalReference(config: NpcConfig): NpcConfig {
        if (!config.entity.providerLinked) return config
        val canonical = renderer.canonicalReference(config) ?: return config
        val stored = config.entity.providerReference
        if (canonical == stored) return config

        logger.info(
            "NPC '${config.id}': its ${config.entity.provider} reference '$stored' is ambiguous; " +
                "storing '$canonical' instead.",
        )

        return config.copy(entity = config.entity.copy(providerReference = canonical))
    }

    fun statusOf(config: NpcConfig, snapshot: CloudSnapshot): NpcStatus =
        statusOf(config, config.targetServers.map { TargetResolver.resolve(it, snapshot) })

    private fun statusOf(config: NpcConfig, resolutions: List<TargetResolution>): NpcStatus = when {
        config.entity.needsRelocation -> NpcStatus.NEEDS_RELOCATION
        config.targetServers.isEmpty() -> NpcStatus.TARGET_MISSING
        resolutions.any { it == TargetResolution.Ambiguous } -> NpcStatus.TARGET_AMBIGUOUS
        resolutions.any { it == TargetResolution.NotFound } -> NpcStatus.TARGET_MISSING
        else -> NpcStatus.COMPLETE
    }

    suspend fun lookupSkin(input: String): NpcConfig.SkinConfiguration? {
        val uuid = InputChecks.parseUuid(input)
        return if (uuid != null) renderer.captureSkinByUuid(uuid) else renderer.captureSkinByUsername(input)
    }

    private fun exists(id: String): Boolean = repository.find(id) != null

    private suspend fun resolveTarget(name: String): TargetLookup =
        when (val resolution = TargetResolver.resolve(name)) {
            is TargetResolution.Found -> TargetLookup.Found(resolution.target)
            TargetResolution.Ambiguous -> TargetLookup.Failed(
                NpcOperationResult.Failure(
                    NpcFailure.TARGET_AMBIGUOUS,
                    name,
                ),
            )

            TargetResolution.NotFound -> TargetLookup.Failed(
                NpcOperationResult.Failure(
                    NpcFailure.TARGET_NOT_FOUND,
                    name,
                ),
            )
        }

    suspend fun create(id: String, targetName: String, player: NpcPlayer): NpcOperationResult {
        validateId(id)?.let { return it }
        if (exists(id)) return NpcOperationResult.Failure(NpcFailure.ALREADY_EXISTS, id)

        val target = when (val lookup = resolveTarget(targetName)) {
            is TargetLookup.Found -> lookup.target
            is TargetLookup.Failed -> return lookup.failure
        }

        val entity = NpcConfig.NpcEntityConfiguration(
            location = player.location(),
            provider = providers.defaultProvider,
            skin = renderer.captureSkin(player),
        )

        return persistNew(createDefaultConfig(id, target.name, entity)).also { result ->
            if (result is NpcOperationResult.Success) {
                val provider = result.config.entity.provider
                logger.info("Created NPC '$id' using provider '$provider' (target: ${target.name}).")
            }
        }
    }

    fun duplicate(sourceId: String, newId: String, location: NpcLocation): NpcOperationResult {
        validateId(newId)?.let { return it }
        if (exists(newId)) return NpcOperationResult.Failure(NpcFailure.ALREADY_EXISTS, newId)
        val source = repository.find(sourceId) ?: return NpcOperationResult.Failure(NpcFailure.NOT_FOUND, sourceId)

        val entity = source.entity
        val ownProvider = !entity.providerLinked && entity.provider.lowercase() in Providers.OWN
        val copy = source.copy(
            id = newId,
            entity = entity.copy(
                location = location,
                provider = if (ownProvider) entity.provider else providers.defaultProvider,
                providerReference = null,
                providerLinked = false,
                needsRelocation = false,
                dormantProviderReferences = emptyMap(),
            ),
        )

        return persistNew(copy).also { result ->
            if (result is NpcOperationResult.Success) logger.info("Duplicated NPC '$sourceId' as '$newId'.")
        }
    }

    suspend fun link(id: String, targetName: String, provider: String, reference: String): NpcOperationResult {
        validateId(id)?.let { return it }
        if (exists(id)) return NpcOperationResult.Failure(NpcFailure.ALREADY_EXISTS, id)

        val target = when (val lookup = resolveTarget(targetName)) {
            is TargetLookup.Found -> lookup.target
            is TargetLookup.Failed -> return lookup.failure
        }

        val providerKey = provider.lowercase()
        if (providerKey !in providers.linkingProviders) {
            return NpcOperationResult.Failure(NpcFailure.PROVIDER_UNAVAILABLE, provider)
        }
        if (repository.findByProvider(providerKey, reference) != null) {
            return NpcOperationResult.Failure(NpcFailure.PROVIDER_NPC_ALREADY_LINKED, reference)
        }

        val entity = NpcConfig.NpcEntityConfiguration(
            provider = providerKey,
            providerReference = reference,
            providerLinked = true,
        )
        val snapshot = renderer.snapshotOf(NpcConfig(id = id, entity = entity))
            ?: return NpcOperationResult.Failure(NpcFailure.PROVIDER_NPC_NOT_FOUND, reference)

        val config = createDefaultConfig(id, target.name, entity.copy(location = snapshot.location ?: NpcLocation()))
            .also { it.hologram.enabled = false }

        return persistNew(config).also { result ->
            if (result is NpcOperationResult.Success) {
                logger.info("Linked NPC '$id' to $providerKey NPC '$reference' (target: ${target.name}).")
            }
        }
    }

    private fun persistNew(config: NpcConfig): NpcOperationResult {
        var spawned: NpcConfig? = null
        return try {
            spawned = renderer.spawn(config)
            repository.save(spawned)
            hologramRenderer.createOrUpdate(spawned)

            NpcOperationResult.Success(spawned)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            val placed = spawned ?: config
            runCatching { hologramRenderer.destroy(placed) }
            runCatching { renderer.despawn(placed) }
            runCatching { repository.delete(config.id) }
            NpcOperationResult.Failure(NpcFailure.CREATE_FAILED, exception.message)
        }
    }

    suspend fun addTarget(id: String, targetName: String): NpcOperationResult {
        val config = repository.find(id) ?: return NpcOperationResult.Failure(NpcFailure.NOT_FOUND, id)
        val target = when (val lookup = resolveTarget(targetName)) {
            is TargetLookup.Found -> lookup.target
            is TargetLookup.Failed -> return lookup.failure
        }
        if (config.targetServers.any { it.equals(target.name, ignoreCase = true) }) {
            return NpcOperationResult.Failure(NpcFailure.TARGET_ALREADY_USED, target.name)
        }
        if (config.targetServers.size >= NpcConfig.MAX_TARGETS) {
            return NpcOperationResult.Failure(NpcFailure.TARGET_LIMIT, NpcConfig.MAX_TARGETS.toString())
        }

        config.targetServers += target.name
        save(config)

        return NpcOperationResult.Success(config)
    }

    fun unlink(id: String): NpcOperationResult {
        val config = repository.find(id) ?: return NpcOperationResult.Failure(NpcFailure.NOT_FOUND, id)
        if (!config.entity.providerLinked) return NpcOperationResult.Failure(NpcFailure.NOT_LINKED, id)

        hologramRenderer.destroy(config)
        renderer.despawn(config)
        repository.delete(config.id)

        return NpcOperationResult.Success(config)
    }

    fun delete(id: String): NpcOperationResult {
        val config = repository.find(id) ?: return NpcOperationResult.Failure(NpcFailure.NOT_FOUND, id)
        if (config.entity.providerLinked) return unlink(id)
        hologramRenderer.destroy(config)
        renderer.despawn(config)

        val remaining = resolveDormantReferences(config, readoptLinkOnly = false).entity.dormantProviderReferences
        if (remaining.isNotEmpty()) {
            val leftovers = remaining.entries.joinToString { (provider, reference) -> "$provider=$reference" }
            logger.warning(
                "Deleted NPC '$id' still has provider-side NPCs that could not be cleaned up " +
                    "because their plugins aren't installed: $leftovers. " +
                    "Remove them manually if those plugins are ever installed again.",
            )
        }

        repository.delete(config.id)

        return NpcOperationResult.Success(config)
    }

    fun reconcileProvider(config: NpcConfig): NpcConfig {
        if (config.entity.providerLinked) return config

        var current = adoptLegacyProviderNpc(resolveDormantReferences(config, readoptLinkOnly = true))
        if (current.entity.needsRelocation) return current

        val legacyReference = current.entity.providerReference
        if (legacyReference != null && current.entity.provider.lowercase() in providers.creationCapableProviders) {
            deleteProviderNpc(current, current.entity.provider.lowercase(), legacyReference)
            current = current.copy(entity = current.entity.copy(providerReference = null))
        }

        val active = current.entity.provider.lowercase()
        val pinned = active in providers.availableProviders && active !in providers.creationCapableProviders
        val target = if (pinned) active else providers.defaultProvider.lowercase()
        if (active == target) return current

        logger.info("NPC '${current.id}': provider auto-selection moves it from '$active' to '$target'.")
        hologramRenderer.destroy(current)

        val reference = current.entity.providerReference
        var dormant = current.entity.dormantProviderReferences
        if (reference != null) {
            if (active in providers.availableProviders) {
                deleteProviderNpc(current, active, reference)
            } else {
                dormant = dormant + (active to reference)
            }
        }

        return current.copy(
            entity = current.entity.copy(
                provider = target,
                providerReference = null,
                dormantProviderReferences = dormant,
            ),
        )
    }

    private fun adoptLegacyProviderNpc(config: NpcConfig): NpcConfig {
        if (!config.entity.needsRelocation) return config

        val storedReference = config.entity.providerReference
        val candidates = if (storedReference != null) {
            listOf(config.entity.provider.lowercase() to storedReference)
        } else {
            providers.creationCapableProviders
                .filter { it != Providers.STANDALONE && it != Providers.MANNEQUIN }
                .map { it to config.id }
        }

        for ((provider, reference) in candidates) {
            if (provider !in providers.availableProviders) continue
            val probe = config.copy(entity = config.entity.copy(provider = provider, providerReference = reference))
            val snapshot = runCatching { renderer.snapshotOf(probe) }
                .onFailure {
                    logger.warning("Could not read $provider NPC '$reference' for NPC '${config.id}': ${it.message}")
                }
                .getOrNull()
                ?: continue

            val location = snapshot.location ?: run {
                logger.warning(
                    "NPC '${config.id}': its $provider NPC '$reference' exists but has no location, " +
                        "keeping the relocation warning.",
                )
                continue
            }

            logger.info("NPC '${config.id}': adopted location, skin and name from $provider NPC '$reference'.")
            return config.copy(
                entity = config.entity.copy(
                    provider = provider,
                    providerReference = reference,
                    location = location,
                    skin = snapshot.skin ?: config.entity.skin,
                    customName = snapshot.customName ?: config.entity.customName,
                    needsRelocation = false,
                ),
            )
        }

        return config
    }

    private fun resolveDormantReferences(config: NpcConfig, readoptLinkOnly: Boolean): NpcConfig {
        var entity = config.entity
        val actionable = entity.dormantProviderReferences.filterKeys { it in providers.availableProviders }
        if (actionable.isEmpty()) return config

        var dormant = entity.dormantProviderReferences - actionable.keys
        actionable.forEach { (provider, reference) ->
            val linkOnly = provider !in providers.creationCapableProviders
            if (linkOnly && readoptLinkOnly) {
                val previousProvider = entity.provider.lowercase()
                val previousReference = entity.providerReference
                if (previousReference != null && previousProvider != provider) {
                    if (previousProvider in providers.availableProviders) {
                        deleteProviderNpc(config, previousProvider, previousReference)
                    } else {
                        dormant = dormant + (previousProvider to previousReference)
                    }
                }

                hologramRenderer.destroy(config.copy(entity = entity))
                logger.info(
                    "NPC '${config.id}': re-adopting its $provider NPC '$reference' now that $provider " +
                        "is installed again.",
                )
                entity = entity.copy(provider = provider, providerReference = reference)
            } else {
                deleteProviderNpc(config, provider, reference)
            }
        }

        return config.copy(entity = entity.copy(dormantProviderReferences = dormant))
    }

    private fun deleteProviderNpc(config: NpcConfig, provider: String, reference: String) {
        logger.info("Cleaning up leftover $provider NPC '$reference' owned by NPC '${config.id}'.")
        runCatching {
            renderer.despawn(
                config.copy(entity = config.entity.copy(provider = provider, providerReference = reference)),
            )
        }.onFailure { logger.warning("Could not clean up leftover $provider NPC '$reference': ${it.message}") }
    }

    fun teleport(id: String, location: NpcLocation): NpcOperationResult {
        val config = repository.find(id) ?: return NpcOperationResult.Failure(NpcFailure.NOT_FOUND, id)
        val moved = config.copy(entity = config.entity.copy(location = location, needsRelocation = false))
        val updated = if (config.entity.needsRelocation) {
            val settled = reconcileProvider(moved)
            renderer.spawn(settled.copy(entity = settled.entity.copy(location = location, needsRelocation = false)))
        } else {
            moved.also { renderer.teleport(it) }
        }

        repository.save(updated)
        hologramRenderer.createOrUpdate(updated)

        return NpcOperationResult.Success(updated)
    }

    fun updateSkin(id: String, skin: NpcConfig.SkinConfiguration): NpcOperationResult {
        val config = repository.find(id) ?: return NpcOperationResult.Failure(NpcFailure.NOT_FOUND, id)
        val updated = config.copy(entity = config.entity.copy(skin = skin))

        repository.save(updated)
        renderer.refresh(updated)

        return NpcOperationResult.Success(updated)
    }

    private fun validateId(id: String): NpcOperationResult.Failure? =
        if (ConfigIds.isValid(id)) null else NpcOperationResult.Failure(NpcFailure.INVALID_ID, id)

    private fun createDefaultConfig(id: String, target: String, entity: NpcConfig.NpcEntityConfiguration): NpcConfig =
        NpcConfig(
            id = id,
            entity = entity,
            targetServers = mutableListOf(target),
            hologram = NpcConfig.HologramConfigurationRoot(
                layouts = mutableListOf(
                    NpcConfig.HologramLayout(
                        joinState = NpcConfig.DEFAULT_JOIN_STATE,
                        lines = mutableListOf(
                            HologramLine("<#0ea5e9><bold><target_name>"),
                            HologramLine("<#94a3b8><target_online_players>/<target_max_players> online"),
                        ),
                    ),
                    NpcConfig.HologramLayout(
                        joinState = "maintenance",
                        lines = mutableListOf(
                            HologramLine("<#dc2626><bold><target_name>"),
                            HologramLine("<#94a3b8>Currently under maintenance"),
                        ),
                    ),
                    NpcConfig.HologramLayout(
                        joinState = JoinStateResolver.OFFLINE,
                        lines = mutableListOf(
                            HologramLine("<#64748b><bold><target_name>"),
                            HologramLine("${Msg.ERROR}<bold>Offline"),
                        ),
                    ),
                ),
            ),
            actions = mutableListOf(
                NpcConfig.ActionConfiguration(
                    interactionType = PlayerInteraction.RIGHT_CLICK,
                    joinState = NpcConfig.DEFAULT_JOIN_STATE,
                    joinTarget = true,
                ),
                NpcConfig.ActionConfiguration(
                    interactionType = PlayerInteraction.RIGHT_CLICK,
                    joinState = "maintenance",
                    sendMessage = "<#dc2626>This server is currently under maintenance.",
                ),
                NpcConfig.ActionConfiguration(
                    interactionType = PlayerInteraction.RIGHT_CLICK,
                    joinState = JoinStateResolver.OFFLINE,
                    sendMessage = "<#dc2626>This server is currently offline.",
                ),
            ),
        )

    private sealed interface TargetLookup {
        data class Found(val target: ResolvedTarget) : TargetLookup
        data class Failed(val failure: NpcOperationResult.Failure) : TargetLookup
    }

    private companion object {
        private const val LOCATION_DRIFT_TOLERANCE = 0.01

        private val logger = NpcLog.logger
    }
}

sealed interface NpcOperationResult {
    data class Success(val config: NpcConfig) : NpcOperationResult
    data class Failure(val failure: NpcFailure, val detail: String? = null) : NpcOperationResult
}

enum class NpcStatus(val label: String) {
    NEEDS_RELOCATION("needs relocation"),
    TARGET_MISSING("target missing"),
    TARGET_AMBIGUOUS("target ambiguous"),
    COMPLETE("complete"),
}

enum class NpcFailure {
    INVALID_ID,
    ALREADY_EXISTS,
    NOT_FOUND,
    TARGET_NOT_FOUND,
    TARGET_AMBIGUOUS,
    TARGET_ALREADY_USED,
    TARGET_LIMIT,
    CREATE_FAILED,
    PROVIDER_UNAVAILABLE,
    PROVIDER_NPC_NOT_FOUND,
    PROVIDER_NPC_ALREADY_LINKED,
    NOT_LINKED,
}
