package app.simplecloud.npc.paper.provider

import app.simplecloud.npc.core.NpcLog
import java.util.logging.Level

object ReflectiveLoader {

    inline fun <reified T> load(
        className: String,
        what: String,
        parameterTypes: Array<Class<*>>,
        arguments: Array<Any?>,
    ): T? = instantiate(className, what, parameterTypes, arguments) as? T

    fun instantiate(className: String, what: String, parameterTypes: Array<Class<*>>, arguments: Array<Any?>): Any? =
        runCatching {
            Class.forName(className).getDeclaredConstructor(*parameterTypes).newInstance(*arguments)
        }.onFailure {
            NpcLog.logger.log(Level.WARNING, "Could not load $what", it)
        }.getOrNull()
}
