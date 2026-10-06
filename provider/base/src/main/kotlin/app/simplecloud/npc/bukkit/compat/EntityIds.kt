package app.simplecloud.npc.bukkit.compat

import org.bukkit.Bukkit
import org.bukkit.UnsafeValues
import org.bukkit.World
import java.lang.reflect.Method

object EntityIds {

    private val perWorld: Method? by lazy {
        runCatching { UnsafeValues::class.java.getMethod("nextEntityId", World::class.java) }.getOrNull()
    }

    private val global: Method? by lazy {
        runCatching { UnsafeValues::class.java.getMethod("nextEntityId") }.getOrNull()
    }

    fun next(world: World? = null): Int {
        val unsafe = Bukkit.getUnsafe()
        perWorld?.let { return it.invoke(unsafe, world ?: Bukkit.getWorlds().first()) as Int }
        val method = global ?: error("This server offers no way to reserve entity ids.")

        return method.invoke(unsafe) as Int
    }
}
