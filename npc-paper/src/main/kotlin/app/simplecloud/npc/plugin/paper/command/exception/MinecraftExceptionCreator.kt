package app.simplecloud.npc.plugin.paper.command.exception

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.incendo.cloud.CommandManager
import org.incendo.cloud.exception.ArgumentParseException
import org.incendo.cloud.exception.InvalidSyntaxException
import org.incendo.cloud.exception.NoPermissionException
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler
import org.incendo.cloud.paper.PaperCommandManager

/**
 * @author Niklas Nieberler
 */

class MinecraftExceptionCreator {

    private val prefix = "<#049dd1>TryHub<dark_gray> ┃<gray>"

    fun create(commandManager: PaperCommandManager<CommandSourceStack>) {
        MinecraftExceptionHandler.createNative<CommandSender>()
            .handler(NoPermissionException::class.java, MinecraftExceptionHandler.createDefaultNoPermissionHandler())
            .handler(ArgumentParseException::class.java) { _, exception -> Component.text("yee ha") }
            //.handler(InvalidSyntaxException::class.java) { _, exception -> getComponentCommandUsage(exception) }
            .registerTo(commandManager as CommandManager<CommandSender>)
    }

    /*
    private fun getComponentCommandUsage(context: ExceptionContext<CommandSender, out Exception>): Component {
        val contextSender = context.context().sender()
        val exceptionMessageSplit = context.exception().message!!.split(":")

        val commandUsage = exceptionMessageSplit[1]
            .replace("<", "(")
            .replace(">", ")")

        return if (commandUsage.contains("|")) {
            getMoreThanOneCommandUsage(contextSender, commandUsage)
        } else {
            text("$prefix Versuche es erneut mit<dark_gray>: <#fffbeb>/$commandUsage")
        }
    }

    private fun getMoreThanOneCommandUsage(audience: Audience, message: String): Component {
        val fancyMessage = message.replace("|", "<#fef3c7> | <#fffbeb>")
        return text("$prefix Hast du folgendes schon einmal ausprobiert<dark_gray>:")
            .append(text("\n"))
            .append(text("$prefix <#fffbeb>$fancyMessage"))
    }

     */

}