package app.simplecloud.npc.shared.placeholder

import app.simplecloud.api.CloudApi
import app.simplecloud.api.persistentserver.PersistentServer
import app.simplecloud.plugin.api.shared.placeholder.argument.ArgumentsResolver
import app.simplecloud.plugin.api.shared.placeholder.argument.PropertiesArgumentsResolver
import app.simplecloud.plugin.api.shared.placeholder.provider.AbstractPlaceholderProvider
import app.simplecloud.plugin.api.shared.placeholder.single.SinglePlaceholderExecutor
import app.simplecloud.plugin.api.shared.placeholder.async.AsyncPlaceholder

class PersistentServerPlaceholderProvider : AbstractPlaceholderProvider<PersistentServer>(
    SinglePersistentServerPlaceholderExecutor()
) {

    override suspend fun getArgumentsResolvers(cloudApi: CloudApi, value: PersistentServer) = listOf<ArgumentsResolver>(
        PropertiesArgumentsResolver(value.properties ?: emptyMap())
    )

}

class SinglePersistentServerPlaceholderExecutor : SinglePlaceholderExecutor<PersistentServer> {

    override fun getAsyncPlaceholders(cloudApi: CloudApi) = listOf<AsyncPlaceholder<PersistentServer>>(
        AsyncPlaceholder("name") { it.name },
        AsyncPlaceholder("type") { it.type },
        AsyncPlaceholder("max_players") { it.maxPlayers },
        AsyncPlaceholder("min_memory") { it.minMemory },
        AsyncPlaceholder("max_memory") { it.maxMemory },
        AsyncPlaceholder("persistent_server_id") { it.persistentServerId },
        AsyncPlaceholder("serverhost_id") { it.serverhostId },
    )

}
