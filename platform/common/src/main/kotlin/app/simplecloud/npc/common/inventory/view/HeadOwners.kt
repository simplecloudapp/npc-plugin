package app.simplecloud.npc.common.inventory.view

import app.simplecloud.npc.common.utils.BackgroundTasks
import app.simplecloud.npc.core.config.NpcConfig.SkinConfiguration
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object HeadOwners {

    private class Entry(val skin: SkinConfiguration?, val fetchedAt: Long)

    private val cache = ConcurrentHashMap<String, Entry>()
    private val fetching = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    var lookup: (suspend (String) -> SkinConfiguration?)? = null

    @Volatile
    var onResolved: () -> Unit = {}

    fun skin(owner: String): SkinConfiguration? {
        val key = owner.trim().lowercase()
        if (key.isEmpty()) return null
        val entry = cache[key]
        if (entry == null || expired(entry)) fetch(key)

        return entry?.skin
    }

    private fun expired(entry: Entry): Boolean {
        val ttl = if (entry.skin == null) MISS_TTL else HIT_TTL
        return System.currentTimeMillis() - entry.fetchedAt > ttl
    }

    private fun fetch(key: String) {
        val lookup = lookup ?: return
        if (!fetching.add(key)) return

        BackgroundTasks.launch {
            try {
                val skin = runCatching { lookup(key) }.getOrNull()
                cache[key] = Entry(skin, System.currentTimeMillis())
                if (skin != null) onResolved()
            } finally {
                fetching.remove(key)
            }
        }
    }

    private val HIT_TTL = 6.hours.inWholeMilliseconds
    private val MISS_TTL = 10.minutes.inWholeMilliseconds
}
