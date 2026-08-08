package app.simplecloud.npc.plugin.paper.command

import app.simplecloud.npc.plugin.paper.command.global.NpcCommand
import app.simplecloud.npc.plugin.paper.command.edit.NpcActionCommand
import app.simplecloud.npc.plugin.paper.command.edit.NpcHologramCommand
import app.simplecloud.npc.plugin.paper.command.edit.NpcPushbackCommand
import app.simplecloud.npc.plugin.paper.command.edit.NpcTargetCommand
import app.simplecloud.npc.shared.namespace.NpcNamespace
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.parser.standard.StringParser

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
            NpcTargetCommand(this.namespace),
            NpcHologramCommand(this.namespace),
            NpcPushbackCommand(this.namespace),
            NpcActionCommand(this.namespace),
        ))
    }

    private fun createCommandManager(): PaperCommandManager<CommandSourceStack> {
        val commandManager = PaperCommandManager.builder()
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildOnEnable(this.javaPlugin)
        commandManager.parserRegistry().registerNamedParser(
            "greedyString",
            StringParser.greedyStringParser<CommandSourceStack>(),
        )
        return commandManager
    }

}
