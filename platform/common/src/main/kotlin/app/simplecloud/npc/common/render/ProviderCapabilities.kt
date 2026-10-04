package app.simplecloud.npc.common.render

import app.simplecloud.npc.core.render.Providers

interface ProviderCapabilities {
    val defaultProvider: String
    val availableProviders: Set<String>
    val creationCapableProviders: Set<String>
    val fixedSkinProviders: Set<String>
    val linkingProviders: Set<String>

    fun linkableReferences(provider: String): List<String>

    companion object {
        val STANDALONE_ONLY: ProviderCapabilities = object : ProviderCapabilities {
            override val defaultProvider: String = Providers.STANDALONE
            override val availableProviders: Set<String> = setOf(Providers.STANDALONE)
            override val creationCapableProviders: Set<String> = setOf(Providers.STANDALONE)
            override val fixedSkinProviders: Set<String> = setOf(Providers.STANDALONE)
            override val linkingProviders: Set<String> = emptySet()
            override fun linkableReferences(provider: String): List<String> = emptyList()
        }
    }
}
