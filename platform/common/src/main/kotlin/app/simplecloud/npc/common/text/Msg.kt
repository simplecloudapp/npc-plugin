package app.simplecloud.npc.common.text

import app.simplecloud.npc.core.platform.NpcCommandSender
import net.kyori.adventure.text.minimessage.MiniMessage

object Msg {
    private const val PREFIX = "<#0ea5e9>⚡ NPC <#475569>|"

    const val ERROR = "<#dc2626>"
    const val WARNING = "<#f59e0b>"
    const val SUCCESS = "<#a3e635>"
    const val INFO = "<#0ea5e9>"

    const val ACCENT = "<#f8fafc>"
    const val MUTED = "<#94a3b8>"
    const val SUBTLE = "<#475569>"

    private const val ICON_ERROR = "✖"
    private const val ICON_WARNING = "⚠"
    private const val ICON_SUCCESS = "✔"
    private const val ICON_INFO = "ℹ"

    private val PLACEHOLDER = Regex.fromLiteral("{}")

    val miniMessage: MiniMessage = MiniMessage.miniMessage()

    fun error(template: String, vararg args: Any?) = compose(ERROR, ICON_ERROR, template, args)
    fun warning(template: String, vararg args: Any?) = compose(WARNING, ICON_WARNING, template, args)
    fun success(template: String, vararg args: Any?) = compose(SUCCESS, ICON_SUCCESS, template, args)
    fun info(template: String, vararg args: Any?) = compose(INFO, ICON_INFO, template, args)

    fun errorPrefix() = prefix(ERROR, ICON_ERROR)
    fun successPrefix() = prefix(SUCCESS, ICON_SUCCESS)
    fun infoPrefix() = prefix(INFO, ICON_INFO)

    private fun prefix(color: String, icon: String) = "$PREFIX $color$icon "

    private fun compose(color: String, icon: String, template: String, args: Array<out Any?>): String =
        prefix(color, icon) + highlight(template, args, color)

    fun highlight(template: String, args: Array<out Any?>, baseColor: String): String {
        if (args.isEmpty()) return template
        var argIndex = 0

        return PLACEHOLDER.replace(template) { match ->
            if (argIndex < args.size) {
                "$ACCENT${miniMessage.escapeTags(args[argIndex++].toString())}$baseColor"
            } else {
                match.value
            }
        }
    }
}

fun String.substitute(substitutions: Map<String, String>): String =
    substitutions.entries.fold(this) { acc, (key, replacement) -> acc.replace(key, replacement) }

fun NpcCommandSender.sendError(template: String, vararg args: Any?) =
    sendMessage(Msg.miniMessage.deserialize(Msg.error(template, *args)))

fun NpcCommandSender.sendWarning(template: String, vararg args: Any?) =
    sendMessage(Msg.miniMessage.deserialize(Msg.warning(template, *args)))

fun NpcCommandSender.sendSuccess(template: String, vararg args: Any?) =
    sendMessage(Msg.miniMessage.deserialize(Msg.success(template, *args)))

fun NpcCommandSender.sendInfo(template: String, vararg args: Any?) =
    sendMessage(Msg.miniMessage.deserialize(Msg.info(template, *args)))
