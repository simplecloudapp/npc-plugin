package app.simplecloud.npc.plugin.paper.command.global

import app.simplecloud.npc.plugin.paper.command.AbstractNpcCommand
import app.simplecloud.npc.plugin.paper.command.commandName
import app.simplecloud.npc.plugin.paper.command.message.CommandMessages
import app.simplecloud.npc.shared.inventory.NpcInventory
import app.simplecloud.npc.shared.inventory.configuration.InventoryRepository
import app.simplecloud.npc.shared.namespace.NpcNamespace
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

/**
 * @author Niklas Nieberler
 */

class NpcCommand(
    namespace: NpcNamespace
) : AbstractNpcCommand(
    namespace
) {

    val inventoryRepository = InventoryRepository()
    init {
        inventoryRepository.load()
    }

    @Command(commandName)
    @Permission("simplecloud.command.npc")
    fun execute(sender: CommandSourceStack) {
        val player = sender.sender as Player
        CommandMessages.sendHelpMessage(player)

        CoroutineScope(Dispatchers.IO).launch {
            inventoryRepository.getAll().forEach {
                NpcInventory(it).open(player)
            }
        }
    }

}