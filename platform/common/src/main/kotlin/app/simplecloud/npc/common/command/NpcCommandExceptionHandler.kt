package app.simplecloud.npc.common.command

import app.simplecloud.npc.common.text.sendError
import app.simplecloud.npc.core.NpcLog
import app.simplecloud.npc.core.cloud.CloudUnavailableException
import app.simplecloud.npc.core.platform.NpcCommandSender
import org.incendo.cloud.CommandManager
import org.incendo.cloud.exception.ArgumentParseException
import org.incendo.cloud.exception.CommandExecutionException
import org.incendo.cloud.exception.InvalidCommandSenderException
import org.incendo.cloud.exception.InvalidSyntaxException
import org.incendo.cloud.exception.NoPermissionException
import org.incendo.cloud.exception.NoSuchCommandException
import org.incendo.cloud.exception.parsing.NumberParseException
import java.util.logging.Level

object NpcCommandExceptionHandler {
    private val logger = NpcLog.logger

    fun <C : NpcCommandSender> registerTo(commandManager: CommandManager<C>) {
        val controller = commandManager.exceptionController()

        controller.registerHandler(NoSuchCommandException::class.java) { context ->
            context.context().sender().sendError(
                "Unknown command {}. Run {} for a list of commands.",
                context.exception().suppliedCommand(),
                "/$COMMAND_LABEL help",
            )
        }

        controller.registerHandler(InvalidSyntaxException::class.java) { context ->
            context.context().sender().sendError(
                "Wrong syntax. Usage: {}",
                "/${context.exception().correctSyntax()}",
            )
        }

        controller.registerHandler(NoPermissionException::class.java) { context ->
            context.context().sender().sendError("You don't have permission to do that.")
        }

        controller.registerHandler(InvalidCommandSenderException::class.java) { context ->
            context.context().sender().sendError(context.exception().message ?: "This command can't be used here.")
        }

        controller.registerHandler(ArgumentParseException::class.java) { context ->
            val cause = context.exception().cause

            if (cause is NumberParseException) {
                context.context().sender().sendError("{} is not a valid {}.", cause.input(), cause.numberType())
            } else {
                context.context().sender().sendError(cause.message ?: "Invalid argument.")
            }
        }

        controller.registerHandler(CommandExecutionException::class.java) { context ->
            generateSequence(context.exception().cause) { it.cause?.takeIf { cause -> cause !== it } }
                .filterIsInstance<CloudUnavailableException>()
                .firstOrNull()
                ?.let { cloudOutage ->
                    context.context().sender().sendError(
                        "{}. Try again once the controller is back.",
                        cloudOutage.message ?: "The SimpleCloud controller is unreachable",
                    )
                    return@registerHandler
                }

            reportUnexpected(
                context.context().sender(),
                "Command failed: /${context.context().rawInput().input()}",
                context.exception().cause,
            )
        }

        controller.registerHandler(Throwable::class.java) { context ->
            val exceptionType = context.exception()::class.qualifiedName
            reportUnexpected(
                context.context().sender(),
                "Unhandled $exceptionType for command: /${context.context().rawInput().input()}",
                context.exception(),
            )
        }
    }

    private fun reportUnexpected(sender: NpcCommandSender, summary: String, cause: Throwable?) {
        val reference = java.lang.Long.toHexString(System.nanoTime()).takeLast(6)

        logger.log(Level.SEVERE, "[$reference] $summary", cause)
        sender.sendError("An unexpected error occurred (ref: {}). Please notify an administrator.", reference)
    }
}
