package app.simplecloud.npc.common.render

import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.location.NpcLocation
import app.simplecloud.npc.core.platform.NpcPlayer
import app.simplecloud.npc.core.render.NpcRenderer
import app.simplecloud.npc.core.render.ProviderNpcSnapshot
import app.simplecloud.npc.core.render.Providers
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

class RoutingNpcRenderer(
    private val renderers: Map<String, NpcRenderer>,
) : NpcRenderer, ProviderCapabilities {

    private val warnedMissingProvider = ConcurrentHashMap.newKeySet<String>()

    override val defaultProvider: String
        get() = if (Providers.MANNEQUIN in renderers) Providers.MANNEQUIN else Providers.STANDALONE

    override val availableProviders: Set<String> get() = renderers.keys

    override val creationCapableProviders: Set<String>
        get() = renderers.filterValues(NpcRenderer::supportsCreation).keys

    override val fixedSkinProviders: Set<String>
        get() = renderers.filterValues(NpcRenderer::supportsFixedSkin).keys

    val providersNotReady: List<String>
        get() = renderers.filterValues { !it.isReady() }.keys.sorted()

    override val linkingProviders: Set<String>
        get() = renderers.filterValues(NpcRenderer::supportsLinking).keys

    override fun linkableReferences(provider: String): List<String> =
        renderers[provider.lowercase()]?.takeIf { it.supportsLinking }?.linkableReferences().orEmpty()

    override fun onEnable() = renderers.values.forEach(NpcRenderer::onEnable)
    override fun onDisable() = renderers.values.forEach(NpcRenderer::onDisable)

    override fun spawn(config: NpcConfig): NpcConfig {
        renderers[providerKey(config)]?.let { return it.spawn(config) }

        if (config.entity.providerLinked) {
            logger.log(
                Level.WARNING,
                "NPC '${config.id}' is linked to a '${config.entity.provider}' NPC, but that plugin isn't " +
                    "installed; the NPC stays inactive until it is.",
            )
            return config
        }

        logger.log(
            Level.WARNING,
            "NPC '${config.id}' used provider '${config.entity.provider}', which is no longer installed; " +
                "switching it to '${Providers.STANDALONE}'. Its provider-side NPC will be cleaned up " +
                "if '${config.entity.provider}' is ever installed again.",
        )

        val dormantReferences = config.entity.dormantProviderReferences +
            listOfNotNull(config.entity.providerReference?.let { config.entity.provider.lowercase() to it })
        val migratedConfig = config.copy(
            entity = config.entity.copy(
                provider = Providers.STANDALONE,
                providerReference = null,
                dormantProviderReferences = dormantReferences,
            ),
        )

        return renderers[Providers.STANDALONE]?.spawn(migratedConfig) ?: migratedConfig
    }

    override fun canonicalReference(config: NpcConfig): String? = rendererFor(config)?.canonicalReference(config)

    override fun despawn(config: NpcConfig) {
        rendererFor(config)?.despawn(config)
    }

    override fun refresh(config: NpcConfig) {
        rendererFor(config)?.refresh(config)
    }

    override fun teleport(config: NpcConfig) {
        rendererFor(config)?.teleport(config)
    }

    override fun locationOf(config: NpcConfig): NpcLocation? = rendererFor(config)?.locationOf(config)

    override fun snapshotOf(config: NpcConfig): ProviderNpcSnapshot? =
        renderers[providerKey(config)]?.snapshotOf(config)

    override fun captureSkin(player: NpcPlayer): NpcConfig.SkinConfiguration =
        renderers[Providers.STANDALONE]?.captureSkin(player)
            ?: NpcConfig.SkinConfiguration(sourcePlayer = player.name)

    override suspend fun captureSkinByUsername(username: String): NpcConfig.SkinConfiguration? =
        renderers[Providers.STANDALONE]?.captureSkinByUsername(username)

    override suspend fun captureSkinByUuid(uuid: UUID): NpcConfig.SkinConfiguration? =
        renderers[Providers.STANDALONE]?.captureSkinByUuid(uuid)

    private fun providerKey(config: NpcConfig): String = config.entity.provider.lowercase()

    private fun rendererFor(config: NpcConfig): NpcRenderer? {
        val warnKey = config.id.lowercase()
        renderers[providerKey(config)]?.let { renderer ->
            warnedMissingProvider.remove(warnKey)
            return renderer
        }

        if (warnedMissingProvider.add(warnKey)) {
            logger.log(
                Level.WARNING,
                "NPC '${config.id}' uses provider '${config.entity.provider}', which isn't currently " +
                    "available (its plugin may not be installed or enabled), skipping.",
            )
        }

        return null
    }

    private companion object {
        private val logger = NpcLog.logger
    }
}
