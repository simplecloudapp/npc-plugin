package app.simplecloud.npc.plugin.paper.command

import app.simplecloud.npc.plugin.paper.command.global.NpcCommand
import app.simplecloud.npc.plugin.paper.command.global.NpcInventoryCommand
import app.simplecloud.npc.plugin.paper.command.global.NpcOptionCommand
import app.simplecloud.npc.plugin.paper.command.interact.*
import app.simplecloud.npc.shared.namespace.NpcNamespace
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager

/**
 * @author Niklas Nieberler
 */

class CommandHandler(
    private val namespace: NpcNamespace,
    private val javaPlugin: JavaPlugin
) {

    private val annotationParser = AnnotationParser(createCommandManager(), CommandSourceStack::class.java)

    fun parseCommands() {
        this.annotationParser.parse(listOf(
            NpcCommand(this.namespace),
            NpcInventoryCommand(this.namespace),
            NpcOptionCommand(this.namespace),
            NpcInteractCommand(this.namespace),
            NpcInteractOptionCommand(this.namespace)
        ))
    }

    private fun createCommandManager(): PaperCommandManager<CommandSourceStack> {
        val commandManager = PaperCommandManager.builder()
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildOnEnable(this.javaPlugin)
        return commandManager
    }

}