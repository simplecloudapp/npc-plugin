package app.simplecloud.npc.common.command

import app.simplecloud.npc.common.plugin.NpcPluginContext
import app.simplecloud.npc.common.text.sendError
import app.simplecloud.npc.common.text.sendSuccess
import app.simplecloud.npc.core.config.NpcConfig
import app.simplecloud.npc.core.interaction.PlayerInteraction
import app.simplecloud.npc.core.inventory.InventoryConfiguration
import app.simplecloud.npc.core.platform.NpcCommandSender
import app.simplecloud.npc.core.platform.NpcPlayer

abstract class AbstractNpcCommand<C : NpcCommandSender>(
    protected val pluginContext: NpcPluginContext,
) {
    protected fun findPlayerInteraction(sender: C, value: String): PlayerInteraction? =
        PlayerInteraction.getOrNull(value) ?: run {
            sender.sendError("Unknown interaction {}.", value)
            null
        }

    protected fun findNpc(sender: C, id: String): NpcConfig? =
        pluginContext.npcRepository.find(id) ?: run {
            sender.sendError("NPC {} was not found.", id)
            null
        }

    protected fun findInventory(sender: C, id: String): InventoryConfiguration? =
        pluginContext.inventoryRepository.find(id) ?: run {
            sender.sendError("Inventory {} was not found.", id)
            null
        }

    protected fun requirePlayer(sender: C): NpcPlayer? =
        sender.asPlayer() ?: run {
            sender.sendError("This command can only be used by a player.")
            null
        }

    protected fun saveConfig(
        sender: C,
        config: NpcConfig,
        successMessage: String,
        vararg args: Any?,
        refreshHologram: Boolean = true,
    ) {
        pluginContext.npcManager.save(config, refreshHologram)
        sender.sendSuccess(successMessage, *args)
    }
}
