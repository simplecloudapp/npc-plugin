package app.simplecloud.npc.core.repository

import app.simplecloud.npc.core.NpcLog
import io.leangen.geantyref.GenericTypeReflector
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type

object LenientEnumSerializer : TypeSerializer<Enum<*>> {

    fun handles(type: Type): Boolean = GenericTypeReflector.erase(type).isEnum

    override fun deserialize(type: Type, node: ConfigurationNode): Enum<*>? {
        val raw = node.raw()?.toString() ?: return null
        val constants = GenericTypeReflector.erase(type).enumConstants.filterIsInstance<Enum<*>>()
        val key = raw.trim().replace('-', '_')

        return constants.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: run {
            NpcLog.logger.warning(
                "Unknown value '$raw' at '${node.path().joinToString(".")}', using the default. " +
                    "Allowed: ${constants.joinToString { it.name }}",
            )
            null
        }
    }

    override fun serialize(type: Type, obj: Enum<*>?, node: ConfigurationNode) {
        node.raw(obj?.name)
    }
}
