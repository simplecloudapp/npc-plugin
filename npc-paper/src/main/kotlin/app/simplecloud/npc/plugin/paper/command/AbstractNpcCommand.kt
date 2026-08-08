package app.simplecloud.npc.plugin.paper.command

import app.simplecloud.npc.shared.action.interaction.PlayerInteraction
import app.simplecloud.npc.shared.config.NpcConfig
import app.simplecloud.npc.shared.namespace.NpcNamespace
import app.simplecloud.plugin.api.shared.extension.text
import org.bukkit.command.CommandSender

abstract class AbstractNpcCommand(
    namespace: NpcNamespace,
) : CommandSuggestions(namespace) {

    fun findPlayerInteraction(sender: CommandSender, value: String): PlayerInteraction? {
        return PlayerInteraction.getOrNull(value) ?: run {
            sender.sendMessage(text("$PREFIX <#dc2626>Unknown interaction <#f8fafc>$value<#dc2626>."))
            null
        }
    }

    fun findNpcConfig(sender: CommandSender, id: String): NpcConfig? {
        return namespace.npcRepository.findBySelector(id) ?: run {
            sender.sendMessage(text("$PREFIX <#dc2626>NPC <#f8fafc>$id <#dc2626>was not found."))
            null
        }
    }

    protected fun saveConfig(
        sender: CommandSender,
        config: NpcConfig,
        successMessage: String,
        refreshHologram: Boolean = true,
    ) {
        namespace.npcRepository.save(config)
        if (refreshHologram) {
            namespace.hologramManager.createOrUpdate(config)
        }
        sender.sendMessage(text("$PREFIX <#a3e635>$successMessage"))
    }
}
