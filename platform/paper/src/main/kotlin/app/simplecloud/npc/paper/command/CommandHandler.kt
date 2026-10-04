package app.simplecloud.npc.paper.command

import app.simplecloud.npc.common.command.NpcCommandExceptionHandler
import app.simplecloud.npc.common.command.NpcCommandRegistrar
import app.simplecloud.npc.common.plugin.NpcPluginContext
import org.bukkit.plugin.java.JavaPlugin
import org.incendo.cloud.annotations.AnnotationParser
import org.incendo.cloud.brigadier.BrigadierSetting
import org.incendo.cloud.execution.ExecutionCoordinator
import org.incendo.cloud.paper.PaperCommandManager
import org.incendo.cloud.parser.standard.StringParser

class CommandHandler(
    private val pluginContext: NpcPluginContext,
    javaPlugin: JavaPlugin,
) {
    private val commandManager = PaperCommandManager
        .builder(PaperCommandSenderMapper(javaPlugin))
        .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
        .buildOnEnable(javaPlugin)

    private val annotationParser = AnnotationParser(commandManager, PaperCommandSender::class.java)

    fun parseCommands() {
        commandManager.parserRegistry().registerNamedParser(
            "greedyString",
            StringParser.greedyStringParser(),
        )

        commandManager.brigadierManager().settings().set(BrigadierSetting.FORCE_EXECUTABLE, true)

        NpcCommandExceptionHandler.registerTo(commandManager)
        annotationParser.parse(NpcCommandRegistrar.commands<PaperCommandSender>(pluginContext))
    }
}
